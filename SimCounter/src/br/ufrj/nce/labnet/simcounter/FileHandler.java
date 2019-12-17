package br.ufrj.nce.labnet.simcounter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHandler {

	// Padrões utilizados para a busca nos logs
    private static final String LEADER_CONV = "LEADER CONV EM BROADCAST = ";
    private static final String GROUP_CREATED = "GRUPO CRIADO = ";
    private static final String GROUP_DESTROYED = "GRUPO DESTRUIDO = ";
    private static final String SIM_END = "SHUTDOWN APPLICATION = ";
    private static final String PATTERN_SEPARATOR = " = ";

    // Padrões de atributos utilizados no JSON
	private static final String GROUP_ID = "groupId";
	private static final String GROUP_SIZE = "groupSize";
	private static final String SIM_TIME = "tempo";
	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "lng";

    // Patterns a serem buscados
    Pattern patternLeaderConv;
    Pattern patternGroupCreated;
    Pattern patternGroupDestroyed;
    Pattern patternSimEnd;

	// Separador
	public static final String SEPARATOR = " ";
	

	// Construtor
	FileHandler () {
        // Preparando as expressões regulares
        patternLeaderConv = Pattern.compile(LEADER_CONV);
        patternGroupCreated = Pattern.compile(GROUP_CREATED);
        patternGroupDestroyed = Pattern.compile(GROUP_DESTROYED);
        patternSimEnd = Pattern.compile(SIM_END);
    }


	// Método utilizado para listar todos os arquivos de um diretório
	public File[] readDir (String dir) {
	
		File logsFolder = new File(dir);
		File[] vehicleFolders = logsFolder.listFiles();
		
		return vehicleFolders;
	
	}
	

	// Método utilizado para ler um arquivo de log e buscar por padrões
	public ArrayList<Group> readAndSearchFile (String file) {

		// Abrindo o arquivo de log e inicializando o vetor de retorno
		File vehicleLogFile = new File (file);
		ArrayList<Group> groups = new ArrayList<>();
		
		try {
			// Lê e trata o arquivo linha por linha
			BufferedReader buffer = new BufferedReader (new FileReader(vehicleLogFile));
			String line = "";
			Group currentGroup = null;

			while ((line = buffer.readLine()) != null) {

				// Procura o padrão de criação de um grupo
				if (patternGroupCreated.matcher(line).find()){
					// Como um grupo novo está sendo criado, o antigo já tem que ter sido terminado
				    if (currentGroup != null) {
				        groups.add(currentGroup);
						System.out.println("Erro um grupo foi criado sem o anterior ter sido destruído");
                    }

				    // Parse do json e criação do novo grupo
				    JSONObject json = toJson(line);
				    if (json.has(LATITUDE) && json.has(LONGITUDE))
						currentGroup = new Group(
								json.getString(GROUP_ID),
								json.getInt(GROUP_SIZE),
								json.getString(SIM_TIME),
								json.get(LATITUDE).toString(),
								json.get(LONGITUDE).toString());
				    else
                    	currentGroup = new Group(
                    			json.getString(GROUP_ID),
								json.getInt(GROUP_SIZE),
								json.getString(SIM_TIME));
                }

				// Procura o padrão de destruição de um grupo
				if (patternGroupDestroyed.matcher(line).find()) {
					JSONObject json = toJson(line);

					// Verifica se não tem um problema de um grupo ter sido destruído duas vezes
					if (currentGroup != null) {
						// Verifica se o grupo corrente é realmente o que está sendo destruído
						if (currentGroup.getGroupId().equals(json.getString(GROUP_ID))) {
							// Destrói o grupo
							if (json.has(LATITUDE) && json.has(LONGITUDE))
								currentGroup.destroyGroup(json.getString(SIM_TIME), json.get(LATITUDE).toString(), json.get(LONGITUDE).toString());
							else
								currentGroup.destroyGroup(json.getString(SIM_TIME));

							// Adiciona o grupo destruído e finalizado na lista, liberando o ponteiro do grupo corrente
							groups.add(currentGroup);
							currentGroup = null;
						} else {
							System.out.println("Erro na sincronia dos grupos");
						}
					} else {
						System.out.println("Erro de grupo destruído nulo");
					}
				}

				// Procura o padrão de final de simulação
				if (patternSimEnd.matcher(line).find()) {
					JSONObject json = toJson(line);

					// Pode ter finalizado a simulação com um grupo ainda em andamento
					if (currentGroup != null) {
						// Verifica se realmente o grupo em andamento ainda não foi finalizado
						if (currentGroup.getEndTime() == null) {
							// Destrói o grupo corrente e indica que ele terminou junto com a saída do mapa do veículo
							if (json.has(LATITUDE) && json.has(LONGITUDE))
								currentGroup.destroyGroup(json.getString(SIM_TIME), json.get(LATITUDE).toString(), json.get(LONGITUDE).toString());
							else
								currentGroup.destroyGroup(json.getString(SIM_TIME));
							currentGroup.setShutdown();

							// Adiciona o grupo destruído e finalizado na lista, liberando o ponteiro do grupo corrente
							groups.add(currentGroup);
							currentGroup = null;
						} else {
							System.out.println("Grupo finalizado de maneira erra pelo final da simulação");
						}
					}
				}

				if (patternLeaderConv.matcher(line).find()) {
					JSONObject json = toJson(line);

					if (currentGroup != null) {
						currentGroup.updateSize(json.getInt(GROUP_SIZE), json.getString(SIM_TIME));
					} else {
						System.out.println("Erro líder enviando convite de grupo sem o mesmo existir");
					}
				}
			
			}
			buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return groups;

	}


	// Método para realizar o parse da linha em um json
	JSONObject toJson (String line) {
		// Realiza o split pelo sinal de =
		String[] terms = line.split(PATTERN_SEPARATOR);

		// Realiza o parse do json
		JSONObject json = new JSONObject(terms[1]);

		return json;
	}
	
}
