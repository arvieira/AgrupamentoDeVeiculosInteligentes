package br.ufrj.nce.labnet.vehicleunit.message;

import com.dcaiti.vsimrti.rti.objects.v2x.EncodedV2XMessage;
import com.dcaiti.vsimrti.rti.objects.v2x.MessageRouting;
import com.dcaiti.vsimrti.rti.objects.v2x.V2XMessage;


// Classe que representa uma mensagem de reposta (RESP)
public final class AnswerMsg extends V2XMessage {

    // Variáveis próprias da classe V2XMessage que está herdando
	private static final long serialVersionUID = 1L;
    private final EncodedV2XMessage encodedV2XMessage;
    private final static long minLen = 128L;

    // Variáveis do protocolo
    private String srcId;
    private String dstId;

    // Construtor
    public AnswerMsg(MessageRouting routing, String src, String dst) {
        // Variáveis setadas devido a herança
        super(routing);
        encodedV2XMessage = new EncodedV2XMessage(16L, minLen);

        // Variáveis do protocolo
        this.srcId = src;
        this.dstId = dst;
    }

    // Método próprio da classe herdada
    @Override
    public EncodedV2XMessage getEncodedV2XMessage() {
        return encodedV2XMessage;
    }

    // Método para impressão de um RESP
    @Override
    public String toString() {
        return "AnswerMsg{" + "SRC=" + srcId + ", DST=" + dstId + '}';
    }
}
