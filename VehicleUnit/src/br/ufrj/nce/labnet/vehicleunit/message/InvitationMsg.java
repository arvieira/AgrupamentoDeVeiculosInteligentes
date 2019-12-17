package br.ufrj.nce.labnet.vehicleunit.message;

import br.ufrj.nce.labnet.vehicleunit.vehicle.Node;
import com.dcaiti.vsimrti.rti.objects.v2x.EncodedV2XMessage;
import com.dcaiti.vsimrti.rti.objects.v2x.MessageRouting;
import com.dcaiti.vsimrti.rti.objects.v2x.V2XMessage;

import java.util.ArrayList;


// Classe que representa uma mensagem de convite (CONV)
public final class InvitationMsg extends V2XMessage {

    // Variáveis próprias da classe V2XMessage que está herdando
	private static final long serialVersionUID = 1L;
    private final EncodedV2XMessage encodedV2XMessage;
    private final static long minLen = 128L;

    // Variáveis do protocolo
    private Node leader;                     // Leader do grupo que está enviando o convite
    private ArrayList<Node> membersList;     // Lista de membros do grupo, representa a LUM
    private final int groupSize;             // Tamanho do grupo, representa o CIG

    // Construtor
    public InvitationMsg(MessageRouting routing, Node leader, ArrayList<Node> membersList) {
        // Variáveis setadas devido a herança
        super(routing);
        encodedV2XMessage = new EncodedV2XMessage(16L, minLen);

        // Variáveis do protocolo
        this.leader = leader;

        // Quando está em SOLE, a lista de membros é null e envia convites
        if (membersList != null) {
            this.groupSize = membersList.size();                               // CIG
            this.membersList = new ArrayList<>(membersList);                   // LUM
        } else {
            this.groupSize = 1;                                                // CIG
            this.membersList = null;                                    // LUM
        }
    }

    // Método para fornecer a lista de membros (LUM)
    public ArrayList<Node> getMembersList() {
        return membersList;
    }

    // Método para fornecer o tamanho do grupo (CIG)
    public int getGroupSize() {
        return groupSize;
    }

    // Método próprio da classe herdada
    @Override
    public EncodedV2XMessage getEncodedV2XMessage() {
        return encodedV2XMessage;
    }

    // Método para impressão de um convite
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InvitationMsg{");
        sb.append("groupSize=").append(groupSize);

        // Append do leader na mensagem
        if (leader != null)
            sb.append(", leader=").append(leader.getAddress().getSourceName());
        else
            sb.append(", leader=").append(leader);

        // Append da lista de membros na mensagem
        if (membersList != null) {
            for (Node temp : membersList) {
                sb.append(", member: ").append(temp.getAddress().getSourceName());
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
