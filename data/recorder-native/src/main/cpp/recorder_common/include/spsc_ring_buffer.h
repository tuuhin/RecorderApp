#ifndef SPSC_RING_BUFFER_H
#define SPSC_RING_BUFFER_H

#include <algorithm>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>
#include <memory>
#include <span>
#include <stdexcept>
#include <type_traits>

template <typename T>
class spsc_ring_buffer final {
    static_assert(std::is_trivially_copyable_v<T>,
                  "spsc_ring_buffer requires a trivially copyable type");

    static_assert(std::is_default_constructible_v<T>,
                  "spsc_ring_buffer requires a default-constructible type");

    static_assert(std::numeric_limits<std::size_t>::digits <=
                      std::numeric_limits<std::uint64_t>::digits,
                  "spsc_ring_buffer requires size_t to fit within uint64_t");

    static_assert(std::atomic<std::uint64_t>::is_always_lock_free,
                  "64-bit atomics must be lock-free on this target");

   public:
    explicit spsc_ring_buffer(std::size_t capacity)
        : _capacity(capacity), _store(allocate_store(capacity)) {}

    ~spsc_ring_buffer() = default;

    spsc_ring_buffer(const spsc_ring_buffer&) = delete;
    spsc_ring_buffer& operator=(const spsc_ring_buffer&) = delete;

    spsc_ring_buffer(spsc_ring_buffer&&) = delete;
    spsc_ring_buffer& operator=(spsc_ring_buffer&&) = delete;

    [[nodiscard]] std::size_t capacity() const noexcept { return _capacity; }

    [[nodiscard]]
    std::size_t available_to_read() const noexcept {
        const auto write = _wIdx.load(std::memory_order_acquire);
        const auto read = _rIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        return std::min(readable, _capacity);
    }

    [[nodiscard]]
    std::size_t available_to_write() const noexcept {
        const auto write = _wIdx.load(std::memory_order_acquire);
        const auto read = _rIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        return readable <= _capacity ? _capacity - readable : 0;
    }

    // Single producer only.
    // Returns the number of elements written.
    std::size_t write(std::span<const T> input) noexcept {
        if (input.empty()) return 0;

        const auto write = _wIdx.load(std::memory_order_relaxed);
        const auto read = _rIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);

        // Protect against an invalid occupancy calculation.
        if (readable > _capacity) return 0;

        const auto writable = _capacity - readable;
        const auto count = std::min(input.size(), writable);

        if (count == 0) return 0;

        const auto offset = _writePos;
        const auto firstCount = std::min(count, _capacity - offset);
        const auto secondCount = count - firstCount;

        std::memcpy(_store.get() + offset, input.data(), firstCount * sizeof(T));

        if (secondCount > 0) {
            std::memcpy(_store.get(), input.data() + firstCount, secondCount * sizeof(T));
        }

        // Advance the producer-owned physical position without
        // relying on the uint64_t counter's modulo operation.
        _writePos = advance(offset, count);

        // Publish completed writes.
        _wIdx.store(write + count, std::memory_order_release);
        return count;
    }

    // Single consumer only.
    // Returns the number of elements read.
    [[nodiscard]]
    std::size_t read(std::span<T> output) noexcept {
        if (output.empty()) return 0;

        const auto read = _rIdx.load(std::memory_order_relaxed);
        const auto write = _wIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        if (readable > _capacity) return 0;

        const auto count = std::min(output.size(), readable);
        if (count == 0) return 0;

        const auto offset = _readPos;
        const auto firstCount = std::min(count, _capacity - offset);
        const auto secondCount = count - firstCount;

        std::memcpy(output.data(), _store.get() + offset, firstCount * sizeof(T));

        if (secondCount > 0)
            std::memcpy(output.data() + firstCount, _store.get(), secondCount * sizeof(T));

        // Advance the consumer-owned physical position.
        _readPos = advance(offset, count);

        // Publish completed reads.
        _rIdx.store(read + count, std::memory_order_release);

        return count;
    }

   private:
    static std::unique_ptr<T[]> allocate_store(std::size_t capacity) {
        if (capacity == 0)
            throw std::invalid_argument("spsc_ring_buffer capacity must be greater than zero");
        return std::make_unique<T[]>(capacity);
    }

    // offset < _capacity and count <= _capacity.
    // Avoid offset + count overflowing size_t.
    [[nodiscard]]
    std::size_t advance(std::size_t offset, std::size_t count) const noexcept {
        const auto remaining = _capacity - offset;
        return count >= remaining ? count - remaining : offset + count;
    }

    const std::size_t _capacity;
    std::unique_ptr<T[]> _store;

    // Accessed exclusively by the producer and consumer, respectively.
    // These are physical positions, always in [0, _capacity).
    std::size_t _writePos{0};
    std::size_t _readPos{0};

    // Monotonically increasing counters used for cross-thread
    // publication and occupancy tracking.
    alignas(64) std::atomic<std::uint64_t> _wIdx{0};
    alignas(64) std::atomic<std::uint64_t> _rIdx{0};
};

#endif  // SPSC_RING_BUFFER_H