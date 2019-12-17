package br.ufrj.nce.labnet.vehicleunit.vehicle;

import br.ufrj.nce.labnet.vehicleunit.message.InvitationMsg;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.CommunicationApplication;
import com.dcaiti.vsimrti.fed.applicationNT.ambassador.simulationUnit.applicationInterfaces.VehicleApplication;


@SuppressWarnings("unused")
public class IntelligentVehicleWitnessNoMerge extends IntelligentVehicleWitness implements VehicleApplication, CommunicationApplication {

    // Método para tratar a mudança de grupo quando ocorrer um merge
    @Override
    protected void mergeChangeGroup(InvitationMsg message) {
    }

}
