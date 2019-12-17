package br.ufrj.nce.labnet.vehicleunit.message;

import br.ufrj.nce.labnet.vehicleunit.vehicle.Node;
import com.dcaiti.vsimrti.rti.objects.v2x.EncodedV2XMessage;
import com.dcaiti.vsimrti.rti.objects.v2x.MessageRouting;
import com.dcaiti.vsimrti.rti.objects.v2x.V2XMessage;

import java.awt.*;
import java.util.ArrayList;


// Classe que representa uma mensagem de acknowledgement (ACK)
public final class AckMsg extends V2XMessage {

    // Variáveis próprias da classe V2XMessage que está herdando
	private static final long serialVersionUID = 1L;
    private final EncodedV2XMessage encodedV2XMessage;
    private final static long minLen = 128L;

    // Variáveis do protocolo
    private String leaderId;                 // Id do líder
    private String memberId;                 // Id do novo membro
    private ArrayList<Node> membersList;     // Lista de membros do grupo, representa a LUM
    private Color groupColor;                // Cor para os membros do grupo do líder

    // Construtor
    public AckMsg(MessageRouting routing, String src, String dst, ArrayList<Node> membersList, Color color) {
        // Variáveis setadas devido a herança
        super(routing);
        encodedV2XMessage = new EncodedV2XMessage(16L, minLen);

        // Variáveis do protocolo
        this.leaderId = src;                                             // Id da origem da mensagem
        this.memberId = dst;                                             // Id do destino da mensagem
        this.groupColor = color;                                         // Cor do grupo definida pelo líder
        if (membersList != null)
            this.membersList = new ArrayList<>(membersList);             // LUM
        else
            this.membersList = null;
    }

    // Método para fornecer a lista de membros
    public ArrayList<Node> getMembersList() {
        return membersList;
    }

    // Método para fornecer a cor do grupo definida pelo líder
    public Color getGroupColor() {
        return groupColor;
    }

    // Método próprio da classe herdada
    @Override
    public EncodedV2XMessage getEncodedV2XMessage() {
        return encodedV2XMessage;
    }

    // Método para impressão de um ACK
    @Override
    public String toString() {
        return "AckMsg{" + "SRC=" + leaderId + ", DST=" + memberId + '}';
    }
}
