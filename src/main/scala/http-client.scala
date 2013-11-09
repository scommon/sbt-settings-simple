package org.scommon.sbt.settings

/**
 * Deliberately simple HTTP client for doing POSTs.
 */
object POST {
  import java.net.URL
  import java.io.{ InputStream, OutputStream, ByteArrayOutputStream }
  import java.net.HttpURLConnection
  import scala.annotation.tailrec

  case class Response(code: Int, headers: Map[String, String], payload: Array[Byte] = Array())

  def apply(uri: String, headers: Map[String, String] = Map(), request: Array[Byte] = Array()): Response = {
    val connection = new URL(uri)
      .openConnection
      .asInstanceOf[HttpURLConnection]

    @tailrec
    def processHeaders(idx: Int, accum: Map[String, String] = Map()): Map[String, String] = {
      val next_key = connection.getHeaderFieldKey(idx)
      if (next_key ne null)
        processHeaders(idx + 1, accum + (next_key -> connection.getHeaderField(next_key)))
      else
        accum
    }

    def copy(in: InputStream, out: OutputStream): Unit = {
      val buffer = new Array[Byte](2048)
      var bytes_read = -1
      while ({ bytes_read = in.read(buffer, 0, buffer.length); bytes_read != -1 }) {
        out.write(buffer, 0, bytes_read)
      }
    }

    def toByteArray(in: InputStream): Array[Byte] = {
      if (in ne null) {
        val out = new ByteArrayOutputStream()
        try {
          copy(in, out)
          out.toByteArray()
        } finally {
          out.close()
        }
      } else {
        Array()
      }
    }

    connection.setRequestMethod("POST")
    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setUseCaches(true)

    for ((key, value) <- headers)
      connection.setRequestProperty(key, value)

    val write_stream = connection.getOutputStream
    try {
      write_stream.write(request)
      write_stream.flush
    } finally {
      if (write_stream ne null)
        write_stream.close
    }

    val response_headers = processHeaders(1)
    val response_code = connection.getResponseCode
    val response_stream =
      if (response_code == 200)
        connection.getInputStream
      else
        connection.getErrorStream

    try {
      Response(response_code, response_headers, toByteArray(response_stream))
    } finally {
      if (response_stream ne null)
        response_stream.close
    }
  }
}
