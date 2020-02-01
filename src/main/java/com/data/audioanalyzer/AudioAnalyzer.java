package com.data.audioanalyzer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.util.StreamUtils;

import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

/**
 * The class AudioAnalyzer. It holds implementation to analyze the audio files.
 */
public class AudioAnalyzer {

	/**
	 * The file encoder
	 */
	private Encoder encoder = new Encoder();

	/**
	 * Gets the audio bytes from audio input stream
	 * 
	 * @param audioInputStream The audio input stream
	 * @return The audio bytes
	 */
	private byte[] getAudioBytes(AudioInputStream audioInputStream) {

		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			StreamUtils.copy(audioInputStream, byteArrayOutputStream);
			return byteArrayOutputStream.toByteArray();
		} catch (IOException ioException) {
			System.out.println("Error converting the audio bytes to byte array.");
		}
		return new byte[0];
	}

	/**
	 * Gets the fourier transform of an audio input file
	 * 
	 * @param inputFile The input file
	 * @return The array of frequencies
	 */
	private int[] fourierTransform(File inputFile) {

		try (AudioInputStream in = AudioSystem.getAudioInputStream(inputFile)) {
			byte[] audioBytes = this.getAudioBytes(in);
			AudioFormat format = in.getFormat();
			int[] frequencies;

			if (format.getSampleSizeInBits() == 16) {
				int samplesLength = audioBytes.length / 2;
				frequencies = new int[samplesLength];
				if (format.isBigEndian()) { // The most significant value in the sequence is stored first
					for (int i = 0; i < samplesLength; ++i) {
						byte msb = audioBytes[i * 2];
						byte lsb = audioBytes[i * 2 + 1];
						frequencies[i] = msb << 8 | (255 & lsb);
					}
				} else { // The least significant value in the sequence is stored first
					for (int i = 0; i < samplesLength; i += 2) {
						byte lsb = audioBytes[i * 2];
						byte msb = audioBytes[i * 2 + 1];
						frequencies[i / 2] = msb << 8 | (255 & lsb);
					}
				}
			} else {
				int samplesLength = audioBytes.length;
				frequencies = new int[samplesLength];
				if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
					for (int i = 0; i < samplesLength; ++i) {
						frequencies[i] = audioBytes[i];
					}
				} else {
					for (int i = 0; i < samplesLength; ++i) {
						frequencies[i] = audioBytes[i] - 128;
					}
				}
			}

			return frequencies;
		} catch (UnsupportedAudioFileException unsupportedAudioFileException) {
			System.out.println("The audio file format is not supported.");
		} catch (IOException ioException) {
			System.out.println("Error reading the audio file.");
		}
		return new int[0];

	}

	/**
	 * Checks if the file format is .wav
	 * 
	 * @param audioFile The audio file
	 * @return if the file format is .wav
	 */
	private boolean isWav(File audioFile) {

		return audioFile.toPath().toString().endsWith(".wav");
	}

	/**
	 * Gets the audio encoding attributes
	 * 
	 * @return The audio encoding attributes
	 */
	private EncodingAttributes getAudioEncodingAttributes() {

		EncodingAttributes encodingAttributes = new EncodingAttributes();
		encodingAttributes.setAudioAttributes(new AudioAttributes());
		return encodingAttributes;
	}

	/**
	 * Converts the audio file to .wav format
	 * 
	 * @param audioFile  The audio file
	 * @param targetFile The target file
	 * @return The converted file
	 */
	private void convertToWav(File audioFile, File targetFile) throws EncoderException {

		encoder.encode(new MultimediaObject(audioFile), targetFile, getAudioEncodingAttributes());
	}

	/**
	 * Generate random file name
	 * 
	 * @return The random file name
	 */
	private String generateRandomFileName() {

		return UUID.randomUUID().toString();
	}

	/**
	 * Deletes the file
	 * 
	 * @param file The file
	 */
	private void deleteFile(File file) {

		try {
			Files.delete(file.toPath());
		} catch (IOException ioException) {
			System.out.println("Error deleting file: " + file.toPath());
		}
	}

	/**
	 * Checks if the sound is observed
	 * 
	 * @param frequencies The frequencies
	 * @return If the sound is observed
	 */
	private boolean soundObserved(int[] frequencies) {

		for (int frequency : frequencies) {
			if (frequency != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the media file is silent
	 * 
	 * @param mediaFile The media file
	 * @return If the media file is silent
	 */
	public boolean isSilent(File mediaFile) {

		boolean isConverted = false;

		if (!isWav(mediaFile)) { // Converting the media file to .wav format
			try {
				File targetFile = new File(this.generateRandomFileName() + ".wav");
				this.convertToWav(mediaFile, targetFile);
				mediaFile = targetFile;
				isConverted = true;
			} catch (EncoderException encoderException) {
				System.out.println("Error converting the file to .wav format.");
				return true;
			}
		}

		int[] frequencies = fourierTransform(mediaFile);

		// Deleting the converted .wav file if present
		if (isConverted) {
			this.deleteFile(mediaFile);
		}
		return soundObserved(frequencies);
	}
}
