package br.ufrj.nce.labnet.simcounter;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Group {

    // Id do grupo
    private String groupId;

    // Tamanhos que o grupo assumiu durante a simulação
    // O inteiro é o tamanho e a string é o momento em que atingiu esse valor
    private ArrayList<Pair<Integer, String>> sizes;

    // Tempo de criação e de extinção do grupo
    private String startTime;
    private String endTime;

    // Coordenadas GPS da criação e destruição do grupo
    private String latCreate = null;
    private String lngCreate = null;
    private String latDestroy = null;
    private String lngDestroy = null;

    // Indica se o grupo finalizou junto com a saída do carro da simulação
    private boolean shutdown;

    // Constantes do JSON
    private static final String GROUP_ID = "groupId";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String SHUTDOWN = "shutdown";
    private static final String LAT_CREATE = "latCreate";
    private static final String LNG_CREATE = "lngCreate";
    private static final String LAT_DESTROY = "latDestroy";
    private static final String LNG_DESTROY = "lngDestroy";
    private static final String MAX_SIZE = "maxSize";
    private static final String MAX_SIZE_TIME = "maxSizeTime";
    private static final String SIZE_ARRAY = "sizeArray";
    private static final String SIZE = "size";
    private static final String SIZE_TIME = "sizeTime";
    private static final String DURATION = "duration";
    private static final String DURATION_SECONDS = "durationSeconds";
    private static final Double SECOND = Double.valueOf(1000000000);


    // Construtor
    public Group(String groupId, int groupSize, String startTime) {
        this.groupId = groupId;
        this.startTime = startTime;
        this.endTime = null;
        this.shutdown = false;

        sizes = new ArrayList<>();
        Pair<Integer, String> size = new Pair<>(groupSize, startTime);            // O "+1"do groupSize é para contar o líder
        sizes.add(size);
    }

    // Construtor com possibilidade de passar as coordenadas GPS
    public Group(String groupId, int groupSize, String startTime, String latCreate, String lngCreate) {
        this.groupId = groupId;
        this.startTime = startTime;
        this.endTime = null;
        this.shutdown = false;

        sizes = new ArrayList<>();
        Pair<Integer, String> size = new Pair<>(groupSize, startTime);            // O "+1"do groupSize é para contar o líder
        sizes.add(size);

        this.latCreate = latCreate;
        this.lngCreate = lngCreate;
    }

    // Método para atualizar o tamanho do grupo
    void updateSize (int groupSize, String time) {
        Pair<Integer, String> registry;
        registry = sizes.get(sizes.size() - 1);

        // Compara o novo valor de tamanho de grupo para saber
        // se houve alteração. Só adiciona a modificação se tiver mudado
        if (registry.getKey() != groupSize) {
            Pair<Integer, String> pair = new Pair<>(groupSize, time);             // O "+1" do groupSize é para contar o líder
            sizes.add(pair);
        }
    }

    // Método para registrar o final do grupo
    void destroyGroup (String time) {
        endTime = time;
    }

    // Método para registrar o final do grupo com a possibilidade de passar coordenadas GPS
    void destroyGroup (String time, String lat, String lng) {
        endTime = time;
        latDestroy = lat;
        lngDestroy = lng;
    }

    // Método para indicar que o grupo terminou juntamente com a simulação
    public void setShutdown() {
        this.shutdown = true;
    }

    // Método para verificar se o grupo terminou dentro do mapa ou não
    public boolean isShutdown() {
        return shutdown;
    }


    // Getters
    public String getGroupId() {
        return groupId;
    }

    public ArrayList<Pair<Integer, String>> getSizes() {
        return sizes;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    // Getters


    // Método para transformar o grupo em um JSON
    // O parâmetro indica se eu quero todos os dados ou se uma versão simplificada
    // sem todos os tamanhos que o grupo assumiu e com a duração já calculada
    public JSONObject toJson(boolean simplify) {
        JSONObject json = new JSONObject();
        json.put(GROUP_ID, groupId);
        json.put(START_TIME, startTime);
        json.put(END_TIME, endTime);
        json.put(SHUTDOWN, shutdown);

        if (latCreate != null
                && lngCreate != null
                && latDestroy != null
                && lngDestroy != null) {
            json.put(LAT_CREATE, latCreate);
            json.put(LNG_CREATE, lngCreate);
            json.put(LAT_DESTROY, latDestroy);
            json.put(LNG_DESTROY, lngDestroy);
        }

        if (sizes != null) {
            JSONArray jsonArray = new JSONArray();
            if (simplify) {
                Pair<Integer, String> current = null;
                for (Pair<Integer, String> temp : sizes) {
                    if (current != null) {
                        if (current.getKey() < temp.getKey()) {
                            current = temp;
                        }
                    } else {
                        current = temp;
                    }
                }

                json.put(MAX_SIZE, current.getKey());
                json.put(MAX_SIZE_TIME, current.getValue());
            } else {
                for (Pair<Integer, String> current : sizes) {
                    JSONObject temp = new JSONObject();
                    temp.put(SIZE, current.getKey());
                    temp.put(SIZE_TIME, current.getValue());
                    jsonArray.put(temp);
                }
                json.put(SIZE_ARRAY, jsonArray);
            }
        }

        if (simplify) {
            Long duration = Long.parseLong(endTime) - Long.parseLong(startTime);
            json.put(DURATION, duration);
            json.put(DURATION_SECONDS, duration/SECOND);
        }

        return json;
    }

    @Override
    public String toString() {
        return toJson(false).toString();
    }

    // Método para gerar o grupo no formato csv
    public String toCsv() {
        JSONObject json = toJson(true);

        //("GroupId,MaxSize,TimeOfMaxSize,StartTime,EndTime,Duration,DurationSeconds,Shutdown\n")
        final StringBuilder sb = new StringBuilder();
        sb.append(json.getString(GROUP_ID)).append(",");
        sb.append(json.getInt(MAX_SIZE)).append(",");
        sb.append(json.getString(MAX_SIZE_TIME)).append(",");
        sb.append(json.getString(START_TIME)).append(",");
        sb.append(json.getString(END_TIME)).append(",");
        sb.append(json.getLong(DURATION)).append(",");
        sb.append(json.getDouble(DURATION_SECONDS)).append(",");
        sb.append(json.getBoolean(SHUTDOWN));
        if (latCreate != null
                && lngCreate != null
                && latDestroy != null
                && lngDestroy != null) {
            sb.append(",");
            sb.append(json.getString(LAT_CREATE)).append(",");
            sb.append(json.getString(LNG_CREATE)).append(",");
            sb.append(json.getString(LAT_DESTROY)).append(",");
            sb.append(json.getString(LNG_DESTROY));
        }

        return sb.toString();
    }
}
