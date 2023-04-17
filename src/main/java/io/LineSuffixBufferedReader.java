package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Appends suffix for lines read.
 */
public class LineSuffixBufferedReader extends BufferedReader {

    boolean eofstream = false;
    int nextPos = -1;
    char[] buffer = null;
    boolean isFirstLine = true;
    private final String suffix;
    private final String firstLineSuffix;
    /**
     * Constructs a new instance.
     *
     * @param in              reader
     * @param suffix          suffix to append
     * @param firstLineSuffix suffix for the first line read
     */
    public LineSuffixBufferedReader(Reader in, String suffix, String firstLineSuffix) {
        super(in);
        if (suffix != null)
            this.suffix = suffix;
        else
            throw new IllegalArgumentException("empty suffix");
        this.firstLineSuffix = firstLineSuffix;
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (eofstream && buffer.length == nextPos)
                return -1;
            return buffer[nextPos++];
        }
    }

    @Override
    public int read(char[] cb, int off, int len) throws IOException {
        synchronized (lock) {
            if ((off < 0) || (off > cb.length) || (len < 0) ||
                    ((off + len) > cb.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            fill(len);

            if (eofstream && buffer.length == nextPos)
                return -1;

            int n = Math.min(len, buffer.length - nextPos);
            System.arraycopy(buffer, nextPos, cb, off, n);
            nextPos += n;
            return n;
        }
    }

    private void fill(int len) throws IOException {

        if (eofstream)
            return;

        if (buffer != null && (nextPos + len) <= buffer.length)
            return;

        StringBuilder sb = new StringBuilder();
        if (buffer != null)
            sb.append(buffer, nextPos, buffer.length - nextPos); //copy any leftover buffer

        while (!eofstream && sb.length() < len) {
            eofstream = !appendSuffixedLine(sb);
        }

        buffer = sb.toString().toCharArray();
        nextPos = 0;

    }

    private boolean appendSuffixedLine(StringBuilder sb) throws IOException {
        String l = super.readLine();
        if (l != null) {
            sb.append(l);
            if (isFirstLine) {
                if (firstLineSuffix != null) {
                    sb.append(firstLineSuffix);
                    isFirstLine = false;
                } else if (!l.isEmpty()) {//don`t append for empty lines
                    sb.append(suffix);
                }
            } else if (!l.isEmpty()) {//don`t append for empty lines
                sb.append(suffix);
            }
            return true;
        }
        return false;
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException();
    }
}
