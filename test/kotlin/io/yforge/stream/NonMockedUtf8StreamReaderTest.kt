package io.yforge.stream

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.charset.StandardCharsets

class Client : StreamReaderClient {

  val messages = arrayListOf<String>()
  var closed = false

  override fun handleMessage(raw: String, from: Utf8StreamReader) {
    messages.add(raw)
  }

  override fun streamClosed(from: Utf8StreamReader) {
    closed = true
  }

}

class Stream : InputStream() {

  private var position = 0
  private var chunkIndex = 0
  private val chunks = arrayOf("Test\nMessages\nBuffered\npart", "ial\n")
    .map { it.toByteArray(StandardCharsets.UTF_8) }

  override fun read(): Int {
    if (chunkIndex < chunks.size) {
      val chunk = chunks[chunkIndex]
      if (position < chunk.size) {
        return chunk[position++].toInt()
      }
      Thread.sleep(20)
      chunkIndex++
      position = 0
      return read()
    }
    return -1
  }

}

internal class NonMockedUtf8StreamReaderTest {

  @Test
  fun `reads multiple complete and partial messages from a single stream chunk`() {
    val client = Client()
    val stream = Stream()
    val uut = Utf8StreamReader(client, stream, "\n")
    uut.waitForShutdown()
    uut.close()
    assertTrue(client.closed)
    assertEquals("Test", client.messages[0])
    assertEquals("Messages", client.messages[1])
    assertEquals("Buffered", client.messages[2])
    assertEquals("partial", client.messages[3])
  }
}
