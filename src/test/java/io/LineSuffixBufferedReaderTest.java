package io;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineSuffixBufferedReaderTest {

    private static final Logger LOGGER = Logger.getLogger(LineSuffixBufferedReaderTest.class);

    private InputStream getInputStream(String text, String suffix, String firstLineSuffix){
        return
                new ReaderInputStream(
                        new LineSuffixBufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))),
                                suffix, firstLineSuffix), StandardCharsets.UTF_8
                );
    }

    private String toString(InputStream io) throws IOException {
        return IOUtils.toString(io, StandardCharsets.UTF_8);
    }

    @Test
    void testEmptySuffix(){
        assertThrows(RuntimeException.class, ()-> getInputStream("", null, ""));
    }

    @Test
    void testCSVWithProperLF() throws IOException {
        String csv = "A,B,C\n1,2,3\n4,5,6\n";
        assertEquals("A,B,C,D\n1,2,3,x\n4,5,6,x\n", toString(getInputStream(csv, ",x\n", ",D\n")));
        assertEquals("A,B,C,x\n1,2,3,x\n4,5,6,x\n", toString(getInputStream(csv, ",x\n", null)));
    }

    @Test
    void testCSVWithMissingLastLF() throws IOException {
        String csv = "A,B,C\n1,2,3\n4,5,6";
        String expected = "A,B,C,D\n1,2,3,x\n4,5,6,x\n";
        assertEquals(expected, toString(getInputStream(csv, ",x\n", ",D\n")));
    }

    @Test
    void testCSVWithMultipleLastLF() throws IOException {
        String csv = "A,B,C\n1,2,3\n4,5,6\n\n\n\n";
        String expected = "A,B,C,D\n1,2,3,x\n4,5,6,x\n";
        assertEquals(expected, toString(getInputStream(csv, ",x\n", ",D\n")));
    }


    @Ignore
    @Test
    void benchmark() throws IOException, NoSuchAlgorithmException {

        //generate 1GB csv file
        File tmpFile  = Files.createTempFile("tmp","csv").toFile();
        tmpFile.deleteOnExit();
        LOGGER.info(tmpFile.getAbsoluteFile());
        FileOutputStream fOut = new FileOutputStream(tmpFile);

        long gb = 1073741824;
        byte[] randoms = new byte[2000];
        Arrays.fill(randoms, (byte)'a');

        long totalBytes = 0;
        long lineCount = 0;
        while(totalBytes < gb) {
            fOut.write(randoms);
            totalBytes+=randoms.length;
            fOut.write('\n');
            totalBytes+=1;
            lineCount+=1;
        }
        fOut.close();
        LOGGER.info(MessageFormat.format("wrote total {0} bytes in {1} lines", totalBytes, lineCount));

        long r1 = readFully(new ReaderInputStream(
                new LineSuffixBufferedReader(new FileReader(tmpFile), ",x\n", ",D\n"), StandardCharsets.UTF_8));

        long r2 = readFully(Files.newInputStream(tmpFile.toPath()));

        long r3 = readFully(new ReaderInputStream(
                new LineSuffixBufferedReader(new FileReader(tmpFile), "\n", null), StandardCharsets.UTF_8));


        assertEquals(r2, r3);
        assertEquals(r2 + 2 * lineCount, r1);

        tmpFile.delete();
    }

    private long readFully(InputStream i) throws IOException, NoSuchAlgorithmException {

        DigestInputStream di = new DigestInputStream(i, MessageDigest.getInstance("SHA-256"));

        long startMillis = System.currentTimeMillis();

        long count = 0;

        byte[] buffer = new byte[5 * 1024 * 1024];
        int l = 0;
        while(true){
            l = di.read(buffer,0, buffer.length);
            if(l == -1)
                break;
            count+=l;
        }

        long endMillis = System.currentTimeMillis();
        String digest = String.valueOf(Base64.getEncoder().encodeToString(di.getMessageDigest().digest()));
        di.close();

        LOGGER.info(MessageFormat.format("read {0} bytes total in {1} seconds, checksum {2}", count, Duration.ofMillis( endMillis - startMillis).getSeconds(),
                digest));

        return count;

    }


}
