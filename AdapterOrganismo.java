package contable.server.domain.adapter;

import java.util.Collection;

import contable.client.domain.OrganismoGwt;
import contable.server.domain.Organismo;

public class AdapterOrganismo {

	public AdapterOrganismo();

	public static OrganismoGwt get(Organismo organismoE){
		return get(organismoE, new OrganismoGwt());
	}


	public static OrganismoGwt get(Organismo organismoE, OrganismoGwt organismoS){

		organismoS.setId(organismoE.getId());
		organismoS.setReparticion(organismoE.getReparticion());
		organismoS.setDependencia(organismoE.getDependencia());
		organismoS.setSeccion(organismoE.getSeccion());
		organismoS.setDomicilio(organismoE.getDomicilio());
		organismoS.setNombre(organismoE.getNombre());

		return organismoS;
	}

	public static Organismo get(OrganismoGwt organismoE, Organismo organismoS){

		organismoS.setId(organismoE.getId());
		organismoS.setReparticion(organismoE.getReparticion());
		organismoS.setDependencia(organismoE.getDependencia());
		organismoS.setSeccion(organismoE.getSeccion());
		organismoS.setDomicilio(organismoE.getDomicilio());
		organismoS.setNombre(organismoE.getNombre());

		return organismoS;
	}
}