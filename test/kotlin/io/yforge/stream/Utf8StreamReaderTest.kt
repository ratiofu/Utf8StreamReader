package io.yforge.stream

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.InputStream
import java.nio.charset.StandardCharsets

private fun stringToBuffer(buffer: ByteArray, content: String): Int {
  val bytes = content.toByteArray(StandardCharsets.UTF_8)
  val length = bytes.size
  if (buffer.size < length) {
    throw Exception(
        "content of byte length $length does not fit in buffer of length ${buffer.size}")
  }
  System.arraycopy(bytes, 0, buffer, 0, length)
  return length
}

@ExtendWith(MockKExtension::class)
internal class Utf8StreamReaderTest {

  @MockK private var client = mockk<StreamReaderClient>()
  @MockK private var stream = mockk<InputStream>()

  @BeforeEach
  fun setUp() {
    every { stream.close() } just Runs
  }

  @Test
  fun `reads multiple complete and partial messages from a single stream chunk`() {
    val cBuffer = slot<ByteArray>()
    var chunk = 0
    every { stream.read(capture(cBuffer), 0, any()) } answers {
      when (chunk) {
        0 -> {
          chunk = 1
          stringToBuffer(cBuffer.captured, "Test\nMessages\nBuffered\npart")
        }
        1 -> {
          chunk = 2
          stringToBuffer(cBuffer.captured, "ial\n")
        }
        else -> -1
      }
    }
    every { stream.available() } returns 0
    every { client.handleMessage(any(), any()) } just Runs
    every { client.streamClosed(any()) } just Runs
    val uut = Utf8StreamReader(client, stream, "\n")
    uut.waitForShutdown()
    verify { client.handleMessage("Test", uut) }
    verify { client.handleMessage("Messages", uut) }
    verify { client.handleMessage("Buffered", uut) }
    verify { client.handleMessage("partial", uut) }
    verify { client.streamClosed(uut) }
    uut.close()
  }
}
