package us.mikeandwan.photos.api.serializers

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.serializers.FormattedInstantSerializer

object InstantSerializer : FormattedInstantSerializer(
    "instant serializer",
    format = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
)

//KSerializer<Calendar> {
//    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Calendar", PrimitiveKind.STRING)
//    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
//
//    override fun serialize(encoder: Encoder, value: Calendar) {
//        encoder.encodeString(formatter.format(value.time))
//    }
//
//    override fun deserialize(decoder: Decoder): Calendar {
//        val dateString = decoder.decodeString()
//        val calendar = Calendar.getInstance()
//
//        calendar.time = formatter.parse(dateString) ?: throw IllegalArgumentException("Invalid date format")
//
//        return calendar
//    }
//}
