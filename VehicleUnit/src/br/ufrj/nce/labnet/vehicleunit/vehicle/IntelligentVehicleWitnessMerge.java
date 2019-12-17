package br.ufrj.nce.labnet.vehicleunit.vehicle;

import br.ufrj.nce.labnet.vehicleunit.message.*;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.CommunicationApplication;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.VehicleApplication;


@SuppressWarnings("unused")
public class IntelligentVehicleWitnessMerge extends IntelligentVehicleWitness implements VehicleApplication, CommunicationApplication {

    // Método para tratar a mudança de grupo quando ocorrer um merge
    @Override
    protected void mergeChangeGroup(InvitationMsg message) {

        // O convite é de um líder estrangeiro, preciso comparar o CIG e me manter no grupo maior
        if (myMembers.size() < message.getGroupSize()) {
            // Passando ao estado de ASP para receber o ACK e se tornar membro do outro grupo
            status = ASP;

            // Enviando resposta do convite do líder estrangeiro
            getLog().infoSimTime(this, "MERGE: Recebido CONV de grupo maior, enviando RESP e aguardando ACK");
            getLog().infoSimTime(this, "CONV: {}", message.toString());
            sendAdHocMessage(message.getRouting().getSourceAddressContainer(), MessageType.ANSWER);

            // Imprime no log o novo status
            getLog().info("ASP");
        }
    }

}
