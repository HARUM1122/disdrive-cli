import okio.buffer
import okio.BufferedSink
import okio.ForwardingSink

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val progressCallback: (Long) -> Unit
) : RequestBody() {
    
    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()
    
    override fun writeTo(sink: okio.BufferedSink) {
        val forwardingSink: ForwardingSink = object : okio.ForwardingSink(sink) {
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                progressCallback(byteCount)
            }
        }
        val bufferedSink: BufferedSink = forwardingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
