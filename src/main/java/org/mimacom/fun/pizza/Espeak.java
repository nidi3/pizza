package org.mimacom.fun.pizza;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: stni
 * Date: 20.06.12
 * Time: 23:15
 * To change this template use File | Settings | File Templates.
 */
public class Espeak {
    private static final String ESPEAK = "espeak";
    private static final String CMD = ESPEAK + "/command_line";
    private static final String OUT_WAV = "out.wav";

    private File dir;
    private final WavConverter wavConverter = new WavConverter();
    private long duration;

    public Espeak() {
        this(new File(System.getProperty("java.io.tmpdir")));
    }

    public Espeak(File dir) {
        this.dir = dir;
    }

    public void installIfNeeded() throws IOException {
        File effective = new File(dir, ESPEAK);
        if (!effective.exists() || !new File(effective, "Readme.txt").exists()) {
            URL espeak = getClass().getClassLoader().getResource(ESPEAK);
            URLConnection con = espeak.openConnection();
            JarURLConnection jarCon = (JarURLConnection) con;
            jarCon.setUseCaches(false);
            JarFile jarFile = jarCon.getJarFile();
            for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = (JarEntry) entries.nextElement();
                String entryPath = entry.getName();
                if (entryPath.startsWith(ESPEAK + "/") && !entry.isDirectory()) {
                    copy(jarFile.getInputStream(entry), new File(dir, entryPath));
                }
            }
        }
    }

    private void copy(InputStream in, File out) throws IOException {
        out.getParentFile().mkdirs();
        FileOutputStream fileout = new FileOutputStream(out);
        copy(in, fileout);
        fileout.close();
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[10000];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
    }

    public void generate(String text, String lang) {
        ProcessBuilder processBuilder = new ProcessBuilder("")
                .directory(execDir())
                .command("cmd", "/c", "espeak", "-v", lang, "-w", "./" + OUT_WAV, "--path=..", text);
        try {
            Process process = processBuilder.start();
            process.waitFor();
//            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, in.getFormat()));
//            line.open(in.getFormat());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void say() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream in = AudioSystem.getAudioInputStream(outputFile());
            clip.open(in);
            in.close();
            clip.start();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (LineUnavailableException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private File execDir() {
        return new File(dir, CMD);
    }

    private File outputFile() {
        return new File(execDir(), OUT_WAV);
    }

    public File convert() throws IOException {
        File skypeOut = new File(execDir(), "skypeOut.wav");
        duration = wavConverter.convertToSkypeWav(outputFile(), skypeOut);
        return skypeOut;
    }

    public long getDuration() {
        return duration;
    }
}
