package com.data.audioanalyzer;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AudioAnalyzerApplication {

	public static void main(String[] args) {

		SpringApplication.run(AudioAnalyzerApplication.class, args);
		AudioAnalyzer analyzer = new AudioAnalyzer();
		System.out.println(analyzer.isSilent(new File("with_audio.flv")));
	}
}
