package org.mimacom.fun.pizza;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: stni
 * Date: 21.06.12
 * Time: 00:46
 * To change this template use File | Settings | File Templates.
 */
public class WavConverter {
    public long convertToSkypeWav(File in, File out) throws IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(in);
            AudioFormat format = ais.getFormat();
            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
            AudioInputStream convIn = AudioSystem.getAudioInputStream(outFormat, ais);
            AudioSystem.write(convIn, AudioFileFormat.Type.WAVE, out);
            ais.close();
            return (long)(1000*ais.getFrameLength()/ais.getFormat().getFrameRate());
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException("Cannot load wav on this system", e);
        }
    }
}