package us.mikeandwan.photos.domain

class NotificationIdRepository {
    private val _lockObject = Any()
    private var _id = 1

    fun getAndInc(): Int {
        synchronized(_lockObject) {
            if (_id == Int.MAX_VALUE) {
                _id = 1
            }

            _id++

            return _id
        }
    }
}
