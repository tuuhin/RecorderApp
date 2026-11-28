#pragma once

#include <cstdint>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <utility>

template <typename T>
class handle_registry_manager final {
   public:
    static constexpr int64_t kInvalidHandle = 0;

    static handle_registry_manager& instance() {
        static handle_registry_manager manager;
        return manager;
    }

    handle_registry_manager(const handle_registry_manager&) = delete;
    handle_registry_manager& operator=(const handle_registry_manager&) = delete;

    int64_t register_handle(std::shared_ptr<T> object) {
        if (!object) return kInvalidHandle;

        std::lock_guard<std::mutex> lock(_lock);
        const int64_t handle = _next_handle++;
        _instances.emplace(handle, std::move(object));
        return handle;
    }

    [[nodiscard]] std::shared_ptr<T> from_handle(int64_t handle) const {
        std::lock_guard<std::mutex> lock(_lock);
        const auto it = _instances.find(handle);
        return it == _instances.end() ? nullptr : it->second;
    }

    std::shared_ptr<T> destroy_handle(int64_t handle) {
        std::shared_ptr<T> victim;
        {
            std::lock_guard<std::mutex> lock(_lock);
            const auto it = _instances.find(handle);
            if (it == _instances.end()) return nullptr;
            victim = std::move(it->second);
            _instances.erase(it);
        }
        // we keep the instance alive while the callers are active
        return victim;
    }

    [[nodiscard]] bool contains(int64_t handle) const {
        std::lock_guard<std::mutex> lock(_lock);
        return _instances.find(handle) != _instances.end();
    }

    [[nodiscard]] std::size_t size() const {
        std::lock_guard<std::mutex> lock(_lock);
        return _instances.size();
    }

   private:
    handle_registry_manager() = default;
    ~handle_registry_manager() = default;

    mutable std::mutex _lock;
    int64_t _next_handle = 1;
    std::unordered_map<int64_t, std::shared_ptr<T>> _instances;
};
