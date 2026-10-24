package com.example.svgmanager.security;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight response wrapper đếm số byte gửi về client mà KHÔNG đệm (buffer) dữ liệu vào bộ nhớ.
 * An toàn O(1) memory kể cả khi download file dung lượng lớn.
 */
public class CountingResponseWrapper extends HttpServletResponseWrapper {

    private final CountingOutputStream countingOutputStream;
    private PrintWriter printWriter;

    public CountingResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        this.countingOutputStream = new CountingOutputStream(response.getOutputStream());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return countingOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (printWriter == null) {
            printWriter = new PrintWriter(new OutputStreamWriter(countingOutputStream, StandardCharsets.UTF_8));
        }
        return printWriter;
    }

    public long getBytesWritten() {
        if (printWriter != null) {
            printWriter.flush();
        }
        return countingOutputStream.getBytesWritten();
    }

    private static class CountingOutputStream extends ServletOutputStream {
        private final ServletOutputStream target;
        private long bytesWritten = 0;

        CountingOutputStream(ServletOutputStream target) {
            this.target = target;
        }

        @Override
        public boolean isReady() {
            return target.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            target.setWriteListener(writeListener);
        }

        @Override
        public void write(int b) throws IOException {
            target.write(b);
            bytesWritten++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            target.write(b, off, len);
            bytesWritten += len;
        }

        @Override
        public void flush() throws IOException {
            target.flush();
        }

        @Override
        public void close() throws IOException {
            target.close();
        }

        long getBytesWritten() {
            return bytesWritten;
        }
    }
}
