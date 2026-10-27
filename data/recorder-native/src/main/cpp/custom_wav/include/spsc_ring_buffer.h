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

   public:
    explicit spsc_ring_buffer(std::size_t capacity)
        : _capacity(capacity), _store(allocate_store(capacity)) {}

    ~spsc_ring_buffer() = default;

    spsc_ring_buffer(const spsc_ring_buffer&) = delete;
    spsc_ring_buffer& operator=(const spsc_ring_buffer&) = delete;

    spsc_ring_buffer(spsc_ring_buffer&&) = delete;
    spsc_ring_buffer& operator=(spsc_ring_buffer&&) = delete;

    [[nodiscard]] std::size_t capacity() const noexcept { return _capacity; }

    [[nodiscard]] std::size_t available_to_read() const noexcept {
        const auto write = _wIdx.load(std::memory_order_acquire);
        const auto read = _rIdx.load(std::memory_order_relaxed);
        return static_cast<std::size_t>(write - read);
    }

    [[nodiscard]] std::size_t available_to_write() const noexcept {
        const auto write = _wIdx.load(std::memory_order_relaxed);
        const auto read = _rIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        return _capacity - readable;
    }

    std::size_t write(std::span<const T> input) noexcept {
        if (input.empty()) return 0;

        const auto write = _wIdx.load(std::memory_order_relaxed);
        const auto read = _rIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        const auto writable = _capacity - readable;
        const auto count = std::min(input.size(), writable);

        if (count == 0) return 0;

        const auto offset = static_cast<std::size_t>(write % _capacity);
        const auto firstCount = std::min(count, _capacity - offset);
        const auto secondCount = count - firstCount;

        // First segment: from the current position to the end.
        std::memcpy(_store.get() + offset, input.data(), firstCount * sizeof(T));

        // Second segment: wrap around to the beginning.
        if (secondCount > 0)
            std::memcpy(_store.get(), input.data() + firstCount, secondCount * sizeof(T));

        // Publish the newly written elements to the consumer.
        _wIdx.store(write + count, std::memory_order_release);

        return count;
    }

    [[nodiscard]] std::size_t read(std::span<T> output) noexcept {
        if (output.empty()) return 0;

        const auto read = _rIdx.load(std::memory_order_relaxed);
        const auto write = _wIdx.load(std::memory_order_acquire);

        const auto readable = static_cast<std::size_t>(write - read);
        const auto count = std::min(output.size(), readable);

        if (count == 0) return 0;

        const auto offset = static_cast<std::size_t>(read % _capacity);
        const auto firstCount = std::min(count, _capacity - offset);
        const auto secondCount = count - firstCount;

        std::memcpy(output.data(), _store.get() + offset, firstCount * sizeof(T));

        if (secondCount > 0)
            std::memcpy(output.data() + firstCount, _store.get(), secondCount * sizeof(T));

        _rIdx.store(read + count, std::memory_order_release);

        return count;
    }

   private:
    static std::unique_ptr<T[]> allocate_store(std::size_t capacity) {
        if (capacity == 0)
            throw std::invalid_argument("spsc_ring_buffer capacity must be greater than zero");

        if (capacity > std::numeric_limits<std::uint64_t>::max())
            throw std::length_error("spsc_ring_buffer capacity exceeds supported counter range");

        return std::make_unique<T[]>(capacity);
    }

    const std::size_t _capacity;
    std::unique_ptr<T[]> _store;

    alignas(64) std::atomic<std::uint64_t> _wIdx{0};
    alignas(64) std::atomic<std::uint64_t> _rIdx{0};
};

#endif  // SPSC_RING_BUFFER_H