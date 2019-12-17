package br.ufrj.nce.labnet.vehicleunit.vehicle;

import br.ufrj.nce.labnet.vehicleunit.message.*;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.CommunicationApplication;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.VehicleApplication;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applications.AbstractApplication;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.communication.AdHocModuleConfiguration;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.operatingSystem.VehicleOperatingSystem;
import com.dcaiti.vsimrti.rti.eventScheduling.Event;
import com.dcaiti.vsimrti.rti.network.AdHocChannel;
import com.dcaiti.vsimrti.rti.objects.TIME;
import com.dcaiti.vsimrti.rti.objects.address.DestinationAddressContainer;
import com.dcaiti.vsimrti.rti.objects.address.SourceAddressContainer;
import com.dcaiti.vsimrti.rti.objects.address.TopocastDestinationAddress;
import com.dcaiti.vsimrti.rti.objects.v2x.AckV2XMessage;
import com.dcaiti.vsimrti.rti.objects.v2x.MessageRouting;
import com.dcaiti.vsimrti.rti.objects.v2x.ReceivedV2XMessage;
import com.dcaiti.vsimrti.rti.objects.v2x.V2XMessage;
import com.dcaiti.vsimrti.rti.objects.vehicle.VehicleInfo;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;


public abstract class IntelligentVehicleWitness extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {

    // Definições relativas ao tempo para envio de convites
    private final static int INVITATION_TIME_INTERVAL = 3;                          // Quantidade de tempo entre o envio dos convites
    private final static long INVITATION_TIME_UNIT = TIME.SECOND;                   // Unidade do tempo para compor o intervalo de convites

    // Definições relativas ao tempo de espera aleatória
    private final static int RANDOM_ANSWER_TIME_INTERVAL = 50;                      // Intervalo de tempo no qual vou sortear um inteiro para esperar
    private final static long  RANDOM_ANSWER_TIME_UNIT = TIME.MILLI_SECOND;			// Unidade de tempo para compor o intervalo de espera aleatória

    // Definições relativas ao tempo para recebimento de ACKs
    private final static int ACK_TIME_INTERVAL = 3;                         // Quantidade de tempo para esperar receber o ACK do RESP
    private final static long ACK_TIME_UNIT = TIME.SECOND;                  // Unidade do tempo para compor o intervalo do ACK

    // Possíveis estados da máquina de estados dos veículos
    String status;
    private final static String SOLE = "sole";
    private final static String LEADER = "leader";
    private final static String WAITING = "waiting";
    final static String ASP = "aspirant";
    private final static String MEMBER = "member";

    // Eventos de temporização
    private Event inviteTimeout;                                            // Timeout para envio de um novo convite
    private Event answerTimeout;                                            // Timeout do temporizador aleatório para envio de uma resposta
    private Event memberKeepAliveTimeout;                                   // Timeout do membro para a espera de convites de seu líder, saindo do grupo
    private Event ackTimeout;                                               // Timeout do ASP para receber um ACK da sua RESP

    // Controle de membros do grupo que o veículo pertence
    private int groupId;                                                    // ID do atual grupo do veículo
    private Node myLeader = null;                                           // Ponteiro para o líder do grupo. Será null se não tem ou se ele mesmo é o líder
    ArrayList<Node> myMembers = null;                                       // Ponteiros para os outros membros do grupo
    private int groupSize;                                                  // Quantidade de integrantes no grupo
    private Color myGroupColor;                                             // Cor do grupo que o veículo faz parte

    // Mensagem armazenada para ser respondida ao final do timeout aleatório
    // Será respondida com uma mensagem RESP
    private ReceivedV2XMessage waitingToAnswerMessage;

    // Definições relativas a cores dos estados e dos diferentes grupos
    private final static Color SOLE_COLOR = Color.WHITE;
    private final static Color LEADER_COLOR = Color.YELLOW;
    private final static Color WAITING_COLOR = Color.ORANGE;
    private final static Color ASP_COLOR = Color.BLACK;
    private final static ArrayList<Color> MEMBER_COLORS  = new ArrayList<>(
            Arrays.asList(
                    Color.MAGENTA,
                    Color.BLUE,
                    Color.CYAN,
                    Color.DARK_GRAY,
                    Color.GRAY,
                    Color.GREEN,
                    Color.LIGHT_GRAY,
                    Color.PINK,
                    Color.RED)
    );

    // Variáveis para construção e trabalho com o SimCounter
    private static final String GROUP_ID = "groupId";
    private static final String GROUP_SIZE = "groupSize";
    private static final String VEHICLE_ID = "vehicleId";
    private static final String LEADER_CONV = "LEADER CONV EM BROADCAST = ";
    private static final String GROUP_CREATED = "GRUPO CRIADO = ";
    private static final String GROUP_DESTROYED = "GRUPO DESTRUIDO = ";
    private static final String SIM_END = "SHUTDOWN APPLICATION = ";
    private static final String SIM_TIME = "tempo";
    private static final String LATITUDE = "lat";
    private static final String LONGITUDE = "lng";
    private static final double LAT_CORRECTION = 0.00145;
    private static final double LNG_CORRECTION = -23.992928;



    // Inicializando o veículo
    @Override
    public void setUp() {
    	// Inicializando a rede wifi do veículo
        getLog().infoSimTime(this, "Initialize application");
        getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
            .addRadio()
            .channel(AdHocChannel.CCH)
            .distance(50)
            .create());
        getLog().infoSimTime(this, "Activated AdHoc Module");

        // Inicializando parâmetros do protocolo
        groupSize = 1;  // O grupo do veículo já começa com 1 integrante que é ele mesmo
        status = SOLE;  // Estado inicial sozinho SOLE
        getLog().info("SOLE"); // Registrando estado inicial SOLE no log
        groupId = 0;    // Primeiro ID de grupo será 0

        // Modifica a cor de todos os carros para SOLE logo após eles entrarem no mapa
        getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(SOLE_COLOR));

        // Inicializa o agendamento de eventos para o envio dos convites
        startInviting();
    }


    // Método chamado quando o veículo inicia e quando o timeout de convite espira
    // Esse timeout só será chamado nos estados SOLE e LEADER, que são aqueles que enviam convites
    private void startInviting() {
        // Cria o novo evento para agendar o próximo envio de convite
        inviteTimeout = new Event(getOperatingSystem().getSimulationTime() + ((new Random().nextInt(INVITATION_TIME_INTERVAL) + 1) * INVITATION_TIME_UNIT), this);
        getOperatingSystem().getEventManager().addEvent(inviteTimeout);

        switch (status) {
            case SOLE:
                // Envia o convite no estado SOLE
                getLog().infoSimTime(this, "Enviando convites CONV em broadcast");
                sendAdHocMessage(null, MessageType.INVITATION);
                break;
            case LEADER:
                // Coloca todos os membros como mortos e espera suas respostas para setar o keepalive de novo
                if (myMembers != null) {
                    for (Node temp : myMembers) {
                        temp.unsetAlive();
                        getLog().info("Marcando como morto para esperar resposta do convite: {}", temp.getAddress().getSourceName());
                    }
                }

                // Montando objeto JSON para escrever no log os dados do grupo
                JSONObject json = new JSONObject();
                json.put(GROUP_ID, getOperatingSystem().getId() + "_" + groupId);
                json.put(GROUP_SIZE, groupSize);
                json.put(SIM_TIME, Long.toString(getOs().getSimulationTime()));

                // Envia o convite no estado LEADER
                getLog().info(LEADER_CONV + json.toString());
                sendAdHocMessage(null, MessageType.INVITATION);
                break;
            default:
        }
    }


    // Método chamado toda vez que um temporizador termina
    @Override
    public void processEvent(Event event) {
        // Timeout para envio de convites
        if (event.equals(inviteTimeout)) {
            handleInviteEvent();
        }

        // Timeout do temporizador aleatório para o envio da mensagem de resposta RESP
        if (event.equals(answerTimeout)) {
            handleAnswerEvent();
        }

        // Timeout do membro, indicando que seu líder saiu do alcance
        if (event.equals(memberKeepAliveTimeout)) {
            handleMemberKeepAliveEvent();
        }

        if (event.equals(ackTimeout)) {
            handleAckTimeoutEvent();
        }
    }


    // Método para tratar o timeout para enviar o próximo convite
    private void handleInviteEvent () {
        // Garantindo que o evento será consumido
        inviteTimeout = null;

        // Estando no estado LEADER, antes de enviar o próximo conjunto de convites, verifico por membros mortos e os retiro
        if (status.equals(LEADER)) {
            getLog().infoSimTime(this, "KeepAliveTimeout!");
            if (myMembers != null) {
                boolean logControl = true;  // Serve para no final da verificação, saber se encontrou algum membro morto
                Iterator<Node> i = myMembers.iterator();
                while (i.hasNext()) {
                    Node temp = i.next();
                    if (!temp.isAlive()) {
                        logControl = false;
                        i.remove();
                        groupSize--;
                        getLog().infoSimTime(this, "Removendo membro morto: {}", temp.getAddress().getSourceName());
                        if (groupSize == 1) {
                            status = SOLE;
                            getLog().info("SOLE");

                            // Montando objeto JSON para escrever no log os dados do grupo
                            JSONObject json = new JSONObject();
                            json.put(GROUP_ID, getOperatingSystem().getId() + "_" + groupId);
                            json.put(GROUP_SIZE, groupSize);
                            json.put(SIM_TIME, Long.toString(getOs().getSimulationTime()));
                            json.put(LATITUDE, getOperatingSystem().getPosition().latitude + LAT_CORRECTION);
                            json.put(LONGITUDE, getOperatingSystem().getPosition().longitude + LNG_CORRECTION);

                            // Escrevendo no log
                            getLog().info(GROUP_DESTROYED + json.toString());

                            groupId++;

                            // Modifica a cor do LEADER de volta para SOLE
                            getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(SOLE_COLOR));

                            // Limpa o ponteiro para o líder
                            myLeader = null;
                        }
                    }
                }
                if (logControl) {
                    getLog().infoSimTime(this, "Nenhum membro morto encontrado");
                }
            }
        }

        // A cada evento de timeout de convites, reagenda o timeout para o envio de convites
        startInviting();
    }


    // Método para tratar o timeout do temporizador aleatório de quando receve um convite
    private void handleAnswerEvent () {
        // Estando no estado de WAITING e tendo o timeout do temporizador aleatório
        // passa ao estado de ASP e envia a resposta a origem
        if (status.equals(WAITING)) {
            status = ASP;

            // Modifica a cor do carro
            getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(ASP_COLOR));

            // Enviando resposta do convite
            getLog().infoSimTime(this, "Timeout, enviando RESP e aguardando ACK");
            sendAdHocMessage(waitingToAnswerMessage.getMessage().getRouting().getSourceAddressContainer(), MessageType.ANSWER);

            // Imprime no log o novo status
            getLog().info("ASP");

            // Garantindo que o evento e a mensagem serão consumidos
            answerTimeout = null;
            waitingToAnswerMessage = null;

            // Agendando o temporizador do ACK, caso não receba a tempo, ele volta para o estado SOLE
            ackTimeout = new Event(getOperatingSystem().getSimulationTime() + ((new Random().nextInt(ACK_TIME_INTERVAL) + 1) * ACK_TIME_UNIT), this);
            getOperatingSystem().getEventManager().addEvent(ackTimeout);
        }
    }


    // Método que um membro vai executar quando não recebeu convites a tempo de seu líder
    private void handleMemberKeepAliveEvent() {
        // O líder sumiu, não pertenço mais a um grupo.
        getLog().infoSimTime(this, "Timeout, líder não responde");
        getLog().infoSimTime(this, "Abandonando grupo");
        status = SOLE;
        getLog().info("SOLE");

        // Modifica a cor do carro
        getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(SOLE_COLOR));

        // Garantindo o consumo do evento e a reinicialização do processo
        myLeader = null;
        myMembers = null;
        groupSize = 1;
        memberKeepAliveTimeout = null;
    }


    // Método que um ASP vai executar quando não recebeu um ACK a tempo
    private void handleAckTimeoutEvent() {
        // Se estou no estado ASP e não recebi o ACK a tempo, volto ao início da máquina de estados como SOLE
        if (status.equals(ASP)) {
            status = SOLE;

            // Modifica a cor do carro
            getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(SOLE_COLOR));

            // Registrando no log o não recebimento do ACK a tempo
            getLog().infoSimTime(this, "Timeout, o ACK para se tornar membro não foi recebido. Reiniciando máquina de estados.");

            // Imprime no log o novo status
            getLog().info("SOLE");

            // Garantindo que o evento temporizador do ACK será consumido
            ackTimeout = null;
        }
    }


    // Método chamado toda vez que uma mensagem é recebida
    @Override
    public void receiveV2XMessage(ReceivedV2XMessage receivedV2XMessage) {
        // Aqui eu vou receber as mensagens e tratá-las
        Object resource = receivedV2XMessage.getMessage();

        // Recebendo um convite (CONV) e tratando
        if (resource instanceof InvitationMsg) {
            handleInvitationMsg(receivedV2XMessage);
        }

        // Recebendo uma resposta (RESP) e tratando
        if (resource instanceof AnswerMsg) {
            handleAnswerMsg(receivedV2XMessage);
        }

        // Recebendo um acknowledgement (ACK) e tratando
        if (resource instanceof AckMsg) {
            handleAckMsg(receivedV2XMessage);
        }

        // Quando um líder recebe um KeepAlive de um membro
        if (resource instanceof KeepAliveMsg) {
            handleKeepAliveMsg(receivedV2XMessage);
        }
    }


    // Método para tratar o recebimento de uma mensagem do tipo convite (CONV)
    private void handleInvitationMsg (ReceivedV2XMessage receivedV2XMessage) {
        // Pega a mesagem do tipo convite de dentro da comunicação recebida
        final InvitationMsg message = (InvitationMsg) receivedV2XMessage.getMessage();

        // Se eu estou no estado SOLE e recebo um convite, passo ao estado WAITING e inicio o temporizador
        if (status.equals(SOLE)) {
            // Registro o recebimento do convite
            getLog().infoSimTime(this, "CONV recebido, iniciando random para responder de {}", receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceName());
            getLog().infoSimTime(this, "CONV: {}", message.toString());

            // Passo ao estado WAITING
            status = WAITING;
            getLog().info("WAITING"); // Registrando WAITING no log

            // Modifica a cor do carro
            getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(WAITING_COLOR));

            // Salvo a mensagem a ser respondida e inicio o timer aleatório para responder ao convite
            waitingToAnswerMessage = receivedV2XMessage;
            answerTimeout = new Event(
                    getOperatingSystem().getSimulationTime() + ((new Random().nextInt(RANDOM_ANSWER_TIME_INTERVAL) + 1) * RANDOM_ANSWER_TIME_UNIT), this);
            getOperatingSystem().getEventManager().addEvent(answerTimeout);
        }

        // Se estou no estado MEMBER
        if (status.equals(MEMBER)) {
            
            if (receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceAddress().equals(myLeader.getAddress().getSourceAddress())
                    && message.getMembersList() == null) {
                getLog().infoSimTime(this, "PROBLEMA: Líder esqueceu o membro, mas o membro não esqueceu o líder");

                // Modifica o status e a cor do carro
                status = SOLE;
                getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(SOLE_COLOR));

                // Reinicialização do processo, como se estivesse abandonando o grupo
                myLeader = null;
                myMembers = null;
                groupSize = 1;

                // trata novamente a mensagem
                handleInvitationMsg(receivedV2XMessage);
            } else {
                // Recebi um convite do meu próprio líder, reseto o timer para sair do grupo e envio o KeepAlive
                if (receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceAddress().equals(myLeader.getAddress().getSourceAddress())) {
                    // Cancelo o timer corrente e crio um novo com o tempo maior que o tempo máximo entre convites.
                    memberKeepAliveTimeout = null;
                    memberKeepAliveTimeout = new Event(getOperatingSystem().getSimulationTime() + ((INVITATION_TIME_INTERVAL + 2) * INVITATION_TIME_UNIT), this);
                    getOperatingSystem().getEventManager().addEvent(memberKeepAliveTimeout);

                    // Registrando o recebimento do pacote de presença do líder
                    getLog().infoSimTime(this, "Recebendo convite do leader como keep alive");
                    getLog().infoSimTime(this, "CONV: {}", message.toString());
                    getLog().infoSimTime(this, "Enviando resposta keep alive para o leader");

                    // Atualizando a lista de membros a partir da mensagem de convite do líder
                    myMembers = new ArrayList<>(message.getMembersList());

                    // Enviar de volta um keepAlive do membro do grupo
                    sendAdHocMessage(receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer(), MessageType.KEEP_ALIVE);
                } else {
                    // Tratar a mudança de grupo no merge
                    mergeChangeGroup(message);
                }
            }
        }
    }


    // Método abstrato para reaizar oa mudança de grupo no merge
    // Será implementado nas classes que realizam o merge, aqui é só um protótipo
    protected abstract void mergeChangeGroup(InvitationMsg message);


    // Método para tratar o recebimento de uma mensagem do tipo resposta (RESP)
    private void handleAnswerMsg(ReceivedV2XMessage receivedV2XMessage) {
        // Pega a mesagem do tipo convite de dentro da comunicação recebida
        final AnswerMsg message = (AnswerMsg) receivedV2XMessage.getMessage();

        if (status.equals(SOLE) || status.equals(WAITING) || status.equals(LEADER)) {
            // Logando a mensagem do tipo RESP recebida
            getLog().infoSimTime(this, "RESP recebido enviando ACK e adicionando ao grupo o {}", receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceName());
            getLog().infoSimTime(this, "Mensagem: {}", message.toString());

            // Daqui em diante é o comportamento comum a todos os estados
            // Atualiza o número de membros no grupo
            groupSize++;

            // Switch da máquina de estados, aqui eu diferencio o comportamento do SOLE, WAITING e LEADER
            switch (status) {
                case SOLE:
                    // Inicializa um novo grupo
                    startGroup();
                    break;
                case WAITING:
                    // Inicializa um novo grupo
                    startGroup();

                    // Cancelar o temporizador aleatório
                    // Garantindo que o evento e a mensagem serão consumidos
                    answerTimeout = null;
                    waitingToAnswerMessage = null;
                    break;
                case LEADER:
                    getLog().infoSimTime(this, "NOVO MEMBRO ADICIONADO: {}", receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceName());
                    break;
                default:
                    getLog().infoSimTime(this, "Erro na máquina de estados ao receber um RESP");
            }

            // Cria o vetor de membros se este não existir e adiciona o novo membro com o alive true
            if (myMembers == null)
                myMembers = new ArrayList<>();
            myMembers.add(new Node(receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer(), true));

            // Envia o ACK aqui
            sendAdHocMessage(receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer(), MessageType.ACK);
        }
    }


    // Método para inicializar o grupo.
    // Utilizado por um líder
    private void startGroup() {
        // Passando ao estado de líder e logando o que ocorreu
        status = LEADER;
        getLog().info("LEADER");

        // Montando objeto JSON para escrever no log os dados do grupo
        JSONObject json = new JSONObject();
        json.put(GROUP_ID, getOperatingSystem().getId() + "_" + groupId);
        json.put(GROUP_SIZE, groupSize);
        json.put(SIM_TIME, Long.toString(getOs().getSimulationTime()));
        json.put(LATITUDE, getOperatingSystem().getPosition().latitude + LAT_CORRECTION);
        json.put(LONGITUDE, getOperatingSystem().getPosition().longitude + LNG_CORRECTION);

        // Escrevendo no log
        getLog().info(GROUP_CREATED + json.toString());

        // Modifica a cor do líder para amarelho
        getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(LEADER_COLOR));

        // Define a cor do grupo aleatóriamente
        myGroupColor = MEMBER_COLORS.get(new Random().nextInt(MEMBER_COLORS.size()));

        // Cria um nó de líder representando ele mesmo e preenche a sua variável de líder
        myLeader = new Node(getOperatingSystem().generateSourceAddressContainer(), true);
    }


    // Método para tratar o recebimento de uma mensagem do tipo acknowledgement (ACK)
    private void handleAckMsg(ReceivedV2XMessage receivedV2XMessage) {
        // Pega a mesagem do tipo convite de dentro da comunicação recebida
        final AckMsg message = (AckMsg) receivedV2XMessage.getMessage();

        // Somente aspirantes se importam com ACK para poder virar membros
        if (status.equals(ASP)) {
            // Passando ao estado de membro
            status = MEMBER;

            // Modifica a cor do veículo
            myGroupColor = message.getGroupColor();
            getOperatingSystem().applyVehicleParametersChange(getOperatingSystem().requestVehicleParametersUpdate().changeColor(myGroupColor));

            // Quando se torna membro de um grupo, pega a referência do seu líder em myleader e coloca o alive em true
            myLeader = new Node (receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer(), true);

            // Criando e populando a lista de membros
            myMembers = new ArrayList<>(message.getMembersList());

            // Registra tudo que aconteceu nos logs
            getLog().infoSimTime(this, "ACK recebido, se tornando membro do grupo {}", receivedV2XMessage.getMessage().getRouting().getSourceAddressContainer().getSourceName());
            getLog().infoSimTime(this, "Mensagem: {}", message.toString());
            getLog().info("MEMBER");

            // Garantindo que o evento temporizador do ACK será consumido
            ackTimeout = null;

            // Garantindo o cancelamento de um possível timer corrente e crio um novo com o tempo maior que o tempo máximo entre convites.
            memberKeepAliveTimeout = null;
            memberKeepAliveTimeout = new Event(getOperatingSystem().getSimulationTime() + ((INVITATION_TIME_INTERVAL + 2) * INVITATION_TIME_UNIT), this);
            getOperatingSystem().getEventManager().addEvent(memberKeepAliveTimeout);
        }
    }


    // Método para invocado por um líder para tratar o recebimento de um KeepAlive de um membro
    private void handleKeepAliveMsg(ReceivedV2XMessage receivedV2XMessage) {
        // Recebendo a mensagem e passando para o tipo correto que é KeepAlive
        final KeepAliveMsg message = (KeepAliveMsg) receivedV2XMessage.getMessage();

        if (status.equals(LEADER)){
            // Registrando o recebimento no log
            getLog().infoSimTime(this, "Recebido keep alive de um membro: {}", message.getRouting().getSourceAddressContainer().getSourceName());
            getLog().infoSimTime(this, "CONV: {}", message.toString());

            // Olhando toda a lista de membros e setando o KeepAlive daquele que enviou a mensagem
            for (Node temp : myMembers) {
                if (temp.getAddress().getSourceAddress().equals(message.getRouting().getSourceAddressContainer().getSourceAddress())) {
                    temp.setAlive();
                    getLog().infoSimTime(this, "Registrando keep alive de um membro: {}", temp.getAddress().getSourceName());
                }
            }
        }
    }


    // Método para o envio de mensagens
    void sendAdHocMessage(SourceAddressContainer origin, MessageType messageType) {
        // Variáveis necessárias para o envio das mensagens
        V2XMessage message = null;
        TopocastDestinationAddress tda;
        DestinationAddressContainer dac;
        MessageRouting routing;

        // Convites são enviados em broadcast, as demais mensagens são em resposta a origem
        if (messageType.equals(MessageType.INVITATION)) {
            // Indica que o destino será broadcast para INVITATION (convites)
            tda = TopocastDestinationAddress.getBroadcastSingleHop();
        } else {
            // Indica que o destino será o endereço de origem do pacote inicial
            tda = new TopocastDestinationAddress(origin.getSourceAddress());
        }

        // Coloca o endereço de destino, junto com o canal wifi em um container
        dac = DestinationAddressContainer.createTopocastDestinationAddressAdHoc(tda, AdHocChannel.CCH);
        // Define a rota da mensagem, indicando o container criado na linha anterior e pegando um endereço padrão para a origem do pacote
        routing = new MessageRouting(dac, getOperatingSystem().generateSourceAddressContainer());

        // O método getVehicleInfo pode retornar null em alguns casos, o que geraria NullPointerException na chamada do
        // getName. Para resolver isso, se eu não tiver informações do veículo para o envio da mensagem, eu mando null
        // mesmo e este será passado para String resolvendo o problema
        VehicleInfo info = getOperatingSystem().getVehicleInfo();
        String srcId;
        if (info != null)
            srcId = getOperatingSystem().getVehicleInfo().getName();
        else
            srcId = null;

        // Adiciona a rota à mensagem, juntamente com o payload da mesma
        switch (messageType) {
            case INVITATION:
                // CONV
                // Cria uma mensagem do tipo convite e coloca no payload a lista de membros (LUM)
                // A própria mensagem calcula o CIG da lista
                // Usada nos estados SOLE e LEADER
                message = new InvitationMsg(routing, myLeader, myMembers);
                break;
            case ANSWER:
                // RESP
                // Responde a origem com uma mensagem do tipo RESP
                // Usada no estado WAITING
                message = new AnswerMsg(routing, srcId, origin.getSourceName());
                break;
            case ACK:
                // ACK
                // Usada no estado SOLE, após receber uma RESP
                message = new AckMsg(routing, srcId, origin.getSourceName(), myMembers, myGroupColor);
                break;
            case KEEP_ALIVE:
                // KeepAlive
                // Mensagem montada pelo MEMBER para mostrar ao LEADER que ainda está no grupo
                message = new KeepAliveMsg(routing, srcId, origin.getSourceName());
                break;
            default:
                // Erro genérico
                getLog().infoSimTime(this, "Mensagem: Tipo desconhecido de mensagem para enviar.");
        }


        // Envio efetivo da mensagem
        getOperatingSystem().getAdHocModule().sendV2XMessage(message);
    }


    // Método para o desligamento da unidade
    @Override
    public void tearDown() {
        // Montando objeto JSON para escrever no log os dados do grupo
        JSONObject json = new JSONObject();
        json.put(VEHICLE_ID, getOperatingSystem().getId());
        json.put(SIM_TIME, Long.toString(getOs().getSimulationTime()));
        json.put(LATITUDE, getOperatingSystem().getPosition().latitude + LAT_CORRECTION);
        json.put(LONGITUDE, getOperatingSystem().getPosition().longitude + LNG_CORRECTION);

        getLog().info(SIM_END + json.toString());
    }


    // Método a ser chamado toda vez que um sensor do veículo mudou seus dados
    @Override
    public void afterUpdateVehicleInfo() {    
    	// Aqui é aonde ele detecta mudanças nos sensores
    	// Após uma modificação dos sensores, ele envia a transação no ledger
    	// Aqui eu não vou programar essa parte, basta logar indicando q realizou uma transação no ledger
    	
    	// if (faz parte de um grupo) {
    	// 	getLog().infoSimTime(this, "Realizando transação no ledger");
    	// }
    	
    	
    	
    	
        /*final List<? extends Application> applications = getOperatingSystem().getApplications();
        final IntraVehicleMsg message = new IntraVehicleMsg(getOperatingSystem().getId(), getRandom().nextInt(0, MAX_ID));

        // Selecionar as informações que serão úteis para o envio em broadcast.
        VehicleOperatingSystem temp = getOperatingSystem();
        String id = temp.getId();					// Identificação do veículo que está enviando as msgs
        GeoPoint position = temp.getPosition();		// Posição GPS do veículo
        VehicleInfo info = temp.getVehicleInfo();        // Heading, speed, route, brake, throttle, stopped 
        VehicleSignals signals = info.getVehicleSignals();	// Luzes e o que o motorista vai fazer.
        IRoadPosition roadPosition = info.getRoadPosition(); // Lane index        
        
        
        // Example usage for how to detect sensor readings
        if (getOperatingSystem().getStateOfEnvironmentSensor(SensorType.Obstacle) > 0) {
            getLog().infoSimTime(this, "Reading sensor");
        }

        for (Application application : applications) {
            final Event event = new Event(getOperatingSystem().getSimulationTime() + 10, application, message);
            this.getOperatingSystem().getEventManager().addEvent(event);
        }*/
    }




    // ########################## Outros Métodos ####################################
    @Override
    public void beforeUpdateConnection() {
    }

    @Override
    public void afterUpdateConnection() {
    }

    @Override
    public void beforeUpdateVehicleInfo() {
    }

    @Override
    public void receiveV2XMessageAcknowledgement(AckV2XMessage ackV2XMessage) {
    }

    @Override
    public void beforeGetAndResetUserTaggedValue() {
    }

    @Override
    public void afterGetAndResetUserTaggedValue() {
    }

    @Override
    public void beforeSendCAM() {
    }

    @Override
    public void afterSendCAM() {
    }

    @Override
    public void beforeSendV2XMessage() {
    }

    @Override
    public void afterSendV2XMessage() {
    }
    // ########################## Outros Métodos ####################################
}
