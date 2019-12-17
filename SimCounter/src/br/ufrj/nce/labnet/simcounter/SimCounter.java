package br.ufrj.nce.labnet.simcounter;

import java.io.File;
import java.util.ArrayList;

public class SimCounter {

	// Definições de nomes de arquivos padrões
	private static final String LOGS_DIR = "/appsNT";
	// Arquivo padrão do simulador que não constitui uma classe do meu programa
	// Esse deve ser descartado para que eu encontre o arquivo de log correto para realizar o parse
	private static final String DISCARD_PATTERN = "OperatingSystem.log";

	// Pasta raiz para realização da contagem
	private String rootFolder;

	// Variáveis que irão receber os resultados
	ArrayList<Group> groups;


	// Construtor da classe
	public SimCounter(String folder) {
		groups = new ArrayList<>();
		rootFolder = folder;
	}


	// Método para realizar a contagem
	protected ArrayList<Group> getGroups () {

		// Abre o diretório o diretório appsNT dentro do resultado da simulação passada
		String vehiclesFolderName = rootFolder + LOGS_DIR;
		FileHandler handler = new FileHandler();
		File[] vehiclesFolder = handler.readDir(vehiclesFolderName);

		// Localiza os arquivos de log
		// Esses arquivos mudam de nome conforme o nome da classe que gera o mesmo na
		// programação das unidades dos veículos no projeto VehicleUnit
		String logFile = getLogFileName(vehiclesFolderName);

		// Realiza um loop por todas as pastas de cada um dos veículos veh_X
		for (int i = 0; i < vehiclesFolder.length; i++) {
			// Adiciona os grupos encontrados em cada arquivo no total de grupos
			groups.addAll(handler.readAndSearchFile(vehiclesFolder[i] + "/" + logFile));
		}

		return groups;

	}


	// Método para identificar o nome dos arquivos de log
	private String getLogFileName (String vehiclesFolderName) {

		String result = "";

		// Lendo a pasta <pasta de log>/appsNT/
		FileHandler handler = new FileHandler();
		File[] vehicleFolderContent = handler.readDir(vehiclesFolderName);

		// Lendo a pasta de um veículo para procurar o nome da classe
		File[] files = handler.readDir(vehicleFolderContent[0].getAbsolutePath());
		for (File file : files) {
			// Pega o arquivo que não está no padrão de descarte
			if (!file.getName().contains(DISCARD_PATTERN))
				result = file.getName();
		}

		return result;

	}

}
