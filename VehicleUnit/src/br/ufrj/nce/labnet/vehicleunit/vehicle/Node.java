package br.ufrj.nce.labnet.vehicleunit.vehicle;

import com.dcaiti.vsimrti.rti.objects.address.SourceAddressContainer;

// Classe que representa um nó qualquer, ou seja, é um veículo
public class Node {

	private SourceAddressContainer address;		// Contém o endereço do nó
	private boolean alive;						// Indicação se o mesmo está próximo ou não

	// Construtor
	Node(SourceAddressContainer address, boolean alive) {
		this.address = address;
		this.alive = alive;
	}

	// Coloca o nó como vivo
	void setAlive() {
		alive = true;
	}

	// Coloca o nó como morto
	void unsetAlive() {
		alive = false;
	}

	// Verifica se o nó está vivo
	boolean isAlive() {
		return alive;
	}

	// Pega o endereço do nó
	public SourceAddressContainer getAddress () {
		return address;
	}

	// Método para imprimir um nó
	@Override
	public String toString() {
		return "Node{" +
				"address=" + address +
				", alive=" + alive +
				'}';
	}
}
