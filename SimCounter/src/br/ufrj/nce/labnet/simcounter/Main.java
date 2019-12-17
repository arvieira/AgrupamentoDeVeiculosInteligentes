package br.ufrj.nce.labnet.simcounter;

import java.io.File;
import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {

		// Imprime o modo de usar, caso não tenha recebido nenhum parâmetro
		if (args.length < 2 || !(new File(args[1]).exists())) {
			printHelp();
			return;
		} else {
			if (args[0].equals("-j")) {
				System.out.println(new SimCounter(args[1]).getGroups());
			} else if (args[0].equals("-c")) {
				System.out.println("GroupId,MaxSize,TimeOfMaxSize,StartTime,EndTime,Duration,DurationSeconds,Shutdown,LatCreate,LngCreate,LatDestroy,LngDestroy");
				for (Group group : new SimCounter(args[1]).getGroups()) {
					// Sem os ifs eu consigo imprimir todos os grupos formados
					if (!group.isShutdown())			// Esse if faz imprimir somente os grupos que terminaram dentro do mapa
					//if (group.isShutdown())			// Esse if faz imprimir somente os grupos que terminaram fora do mapa
						System.out.println(group.toCsv());
				}
			} else {
				printHelp();
			}
		}

	}

	static void printHelp() {
		System.out.println("Usage: java -jar SimCounter.jar [-j|-c] <logs folder>");
		System.out.println("-j\tTo generate a complete JSON file");
		System.out.println("-c\tTo generate a resumed CSV file");
	}

}
