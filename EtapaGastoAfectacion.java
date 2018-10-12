package movimientos.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import contable.client.domain.InfoSaldoPartidaGwt;
import contable.client.domain.RespuestaGeneral;
import contable.server.constants.MovimientoConstants;
import contable.server.dao.ConceptoGastoDAO;
import contable.server.dao.CuentaDAO;
import contable.server.dao.DecisionAdministrativaDAO;
import contable.server.dao.V_SaldoAfectadoExpedienteDAO;
import contable.server.dao.V_SaldoAutorizadoLimitativaDAO;
import contable.server.domain.AfectacionProvisional;
import contable.server.domain.AfectacionProvisionalDet;
import contable.server.domain.Cuenta;
import contable.server.domain.Ejercicio;
import contable.server.domain.Movimiento;
import contable.server.domain.MovimientoDet;
import contable.server.domain.MovimientoDetCuentaPresupuesto;
import contable.server.domain.PartidaAnio;
import contable.server.domain.V_SaldoAutorizadoLimitativa;
import contable.server.domain.adapter.AdapterPartida;
import contable.server.exception.GwtMovimientoException;
import contable.server.exception.GwtPartidaInexistenteMovimientoException;
import contable.server.exception.GwtSaldoParidaMovimientoException;
import contable.server.utils.movimientos.RespuestaMovimiento;
import contable.server.utils.movimientos.presupuestarios.CuentasMovimientoPresupuesto;

/**
 * 
 * @author Max Ariosti
 *
 */
@Deprecated
public abstract class EtapaGastoAfectacion{

	
	/**
	 * Crea los movimientos necesarios para la <B>AFECTACION PROVISIONAL</b>.<br>
	 * Valida que las partidas estén en la DA y que el saldo por limitativa sea correcto.<br>
	 * Se encarga de tomar los items de la afectación, dividirlos en actuales y futuros de manera 
	 * que el movimiento registre en <i><b>recurso_afectado</i></b> o <i><b>recurso_afectado_a_futuro</i></b>  
	 * @param afectacion
	 * @return
	 * @throws GwtMovimientoException
	 * @throws GwtPartidaInexistenteMovimientoException La partida no existe en la Decisión Administrativa actual
	 * @throws GwtSaldoParidaMovimientoException No hay saldo en la partida para afectar
	 */
	public static RespuestaMovimiento crearMovimiento(AfectacionProvisional afectacion)  throws GwtMovimientoException , GwtPartidaInexistenteMovimientoException, GwtSaldoParidaMovimientoException {
		
		CuentasMovimientoPresupuesto cuentasActuales = new CuentasMovimientoPresupuesto(CuentaDAO.PRE_GASTOS_AUTORIZADO, CuentaDAO.PRE_RECUSOS_AFECTADOS);
		CuentasMovimientoPresupuesto cuentasFuturas = new CuentasMovimientoPresupuesto(CuentaDAO.PRE_CREDITO_INICIAL_A_FUTURO, CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO);
		
		//No debería ocurrir que llegue una partida que no esta en la DA porque deberia validarlo el widgete
		//pero por la dudas lo controlamos y en caso de ocurrir tiramos una runtime para que corte la ejecución del sistema
		consultarExistePartidaEnDA(afectacion);
		
		RespuestaMovimiento rta = validarAfectacion(afectacion);
		if(!rta.isOk())
			return rta;

		rta.setOk(crearMovimiento(afectacion, cuentasActuales, cuentasFuturas ));

		return rta;
	}
	
	/**
	 * Crea los movimientos necesarios para la <b>DESAFECTACION PROVISIONAL</b>.<br>
	 * Se encarga de tomar los items de la afectación dividirlos en actuales y futuros de manera 
	 * que el movimiento se registre en <b><i>gasto_autorizado</i></b> o <b><i>credito_inicial_a_futuro</i></b> 
	 * @param afectacion
	 * @return
	 * @throws GwtMovimientoException
	 */
	public static RespuestaMovimiento deshacerMovimiento(AfectacionProvisional afectacion)  throws GwtMovimientoException {
//		RespuestaMovimiento rta = new RespuestaMovimiento();
		CuentasMovimientoPresupuesto cuentasActuales = new CuentasMovimientoPresupuesto(CuentaDAO.PRE_RECUSOS_AFECTADOS,CuentaDAO.PRE_GASTOS_AUTORIZADO);
		CuentasMovimientoPresupuesto cuentasFuturas = new CuentasMovimientoPresupuesto(CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO,CuentaDAO.PRE_CREDITO_INICIAL_A_FUTURO);
		
		
		//TODO consulta de saldo y validaciones para poder deshacer la afectacion
		RespuestaMovimiento rta = validarDesafectar(afectacion);
		if(!rta.isOk())
			return rta;
		
		rta.setOk(crearMovimiento(afectacion, cuentasActuales, cuentasFuturas ));

		return rta;
	}
	
	/**
	 * 
	 * @param afectacion
	 * @param cuentasActuales
	 * @param cuentasFuturas
	 * @return
	 * @throws GwtMovimientoException  Imposibilidad de crear el movimiento ya sea porque no tengo suma 0 en los asientos o un item a efectar tiene ejercicio anterior al actual
	 */
	private static Movimiento crearMovimiento(AfectacionProvisional afectacion, CuentasMovimientoPresupuesto cuentasActuales, CuentasMovimientoPresupuesto cuentasFuturas )throws GwtMovimientoException{

		Movimiento movimiento = new Movimiento(afectacion);

		AfectacionProvisional afectacionActual = new AfectacionProvisional(afectacion);
		AfectacionProvisional afectacionFuturo = new AfectacionProvisional(afectacion);
		
		separarAfectacionPorEjercicio(afectacion, afectacionActual, afectacionFuturo);

		Set<MovimientoDet> movimientosActuales = crearMovimientosPresupuestarios(movimiento,afectacionActual,cuentasActuales.getCuentaOrigen(), cuentasActuales.getCuentaDestino() );
		Set<MovimientoDet> movimientosFuturos = crearMovimientosPresupuestarios(movimiento,afectacionFuturo,cuentasFuturas.getCuentaOrigen(), cuentasFuturas.getCuentaDestino() );
		
		movimientosActuales.addAll(movimientosFuturos);
		movimiento.setMovimientos(movimientosActuales);	
		movimiento.setObjetoCasoDeUso(afectacion.getIdObjetoCasoDeUso());
		movimiento.save();
		
		//marea betty
		afectacion.save();
		
		return movimiento;
	}
	
	/**
	 * Permite a partir de un item de afectación provisional determinemos si corresponde al ejercicio actual o uno futuro.
	 * @param afectacion
	 * @param afectacionActual
	 * @param afectacionFuturo
	 * @throws GwtMovimientoException El item de la afectación provisional es de un ejercicio anterior al actual.
	 */
	private static void separarAfectacionPorEjercicio(AfectacionProvisional afectacion, AfectacionProvisional afectacionActual, AfectacionProvisional afectacionFuturo) throws GwtMovimientoException{
		for (AfectacionProvisionalDet item : afectacion.getItems()) {
			
			if(item.getAnio().intValue() == afectacion.getEjercicio().getAnio().intValue()){
				afectacionActual.addItem(item);
			}else if(item.getAnio().intValue() > afectacion.getEjercicio().getAnio().intValue()){
				afectacionFuturo.addItem(item);
			}else{
				throw new GwtMovimientoException("separarAfectacionPorEjercicio - año del item menor al de la afectacion");
			}
		}
	}
	
	
	/**
	 * 
	 * @param movimiento
	 * @param afectacion
	 * @param cuentaOrigen
	 * @param cuentaDestino
	 * @return
	 * @throws GwtMovimientoException Si la suma de los importes de la cuenta de Origen + los importes de la cuenta Destino no son 0 (cero)
	 */
	private static Set<MovimientoDet> crearMovimientosPresupuestarios( Movimiento movimiento, AfectacionProvisional afectacion, Cuenta cuentaOrigen, Cuenta cuentaDestino) throws GwtMovimientoException {
		//veo cuanto va acumulando, despues borrarlo.
		Set<MovimientoDet> movimientos = movimiento.getMovimientos();
		
//		Double acumPos = new Double(0);
//		Double acumNeg = new Double(0);
//		for (AfectacionProvisionalDet item : afectacion.getItems()) {
//			
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = MovimientosUtils.crearMovDetCuentaPresupuestaria(cuentaOrigen,item.getPartidaAnio(), MovimientoConstants.POSITIVO);
//			movDetCuentaPresupuestaria.setExpediente(afectacion.getExpediente());
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumPos += (MovimientoConstants.POSITIVO*item.getImporte());
//		}
//
//		for (AfectacionProvisionalDet item : afectacion.getItems()) {
//
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = MovimientosUtils.crearMovDetCuentaPresupuestaria(cuentaDestino,item.getPartidaAnio(), MovimientoConstants.NEGATIVO);
//			movDetCuentaPresupuestaria.setExpediente(afectacion.getExpediente());			
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumNeg += (MovimientoConstants.NEGATIVO*item.getImporte());
//		}
//
//		
//		if((acumPos + acumNeg) != 0) 
//			throw new GwtMovimientoException("crearMovimientosPresupuestarios - Afectacion - La suma de los importes no es cero es: " + (acumPos + acumNeg));
//		
		return movimientos;
	}
	
	
	
	
	
	/**
	 * controlamos que las partidas que se vayan a agrupar existan primero en la Decision Admin.
	 * @param afectacion
	 * @throws GwtPartidaInexistenteMovimientoException La partida incluida en la Afectación Provisional no existe en la Decisión Administrativa Actual
	 * 
	 */
	private static RespuestaGeneral consultarExistePartidaEnDA(AfectacionProvisional afectacion) throws GwtPartidaInexistenteMovimientoException{
		Ejercicio ejercicioActual = afectacion.getEjercicio();
		List<PartidaAnio> listaPartidas = afectacion.getItemsPartidaAnio();
		
		RespuestaGeneral rta = new RespuestaGeneral();
		InfoSaldoPartidaGwt infoSaldoGwt = new InfoSaldoPartidaGwt();

		for (PartidaAnio partidaAnio : listaPartidas) {
			if(ejercicioActual.getAnio().intValue() == partidaAnio.getAnio().intValue())
				if(!DecisionAdministrativaDAO.getInstance().isExistePartida(ejercicioActual,partidaAnio)){
					infoSaldoGwt.addPartidaInexistente(AdapterPartida.get(partidaAnio));
				}
		}
		
		if (infoSaldoGwt.getListaPartidasInexistentes().size() > 0) {
			rta.setError("Hay Partidas que no están incluidas en las DA");
			rta.setBusinessObjectGwt(infoSaldoGwt);
			throw new GwtPartidaInexistenteMovimientoException("ERROR GRAVE en consultarExistePartidaEnDA: Las partidas ingresadas no se encuentran en la DA actual => no es posibles realizar el movimiento contable: " + infoSaldoGwt.getMensaje()); 
		}else{
			rta.setOk();
		}
			
		
		
		return rta;
	}
	
	/**
	 * Permite calcular el saldo de las partidas a afectar en función de su limitativa
	 * @param afectacion
	 * @return
	 * @throws GwtSaldoParidaMovimientoException Se da esta situación cuando la partida por la cual se está consultado no existe en la DA actual
	 */
	private static RespuestaMovimiento validarAfectacion(AfectacionProvisional afectacion)  throws GwtSaldoParidaMovimientoException{
		Ejercicio ejercicioActual = afectacion.getEjercicio();
		List<PartidaAnio> listaPartidas = afectacion.getItemsPartidaAnio();
		
		RespuestaMovimiento rta = new RespuestaMovimiento();
		InfoSaldoPartidaGwt infoSaldoGwt = new InfoSaldoPartidaGwt();

		//si bien listaPartidas tiene las partidas agrupadas en funcion de los items de Lic o OC que se van a afectar es
		//necesario que estas partidas que el saldo se calcule en funcion de la limitativa de la partida. 
		List<PartidaAnio> partidasPorLimitativa = agruparPartidasPorLimitativa(listaPartidas);

		// calcular saldo limitativa por partida
		for (PartidaAnio partidaAnio : partidasPorLimitativa) {

			//Si es negativo tengo que desafectar, si es positivo tengo que reforzar la afectación realizada con anterioridad, si es cero no tengo que hacer nada porq el inicio es igual al adj.
			if( partidaAnio.getImporte() != 0){

				//si es el año actual y tengo afectacion calculo saldo ( si es negativo desafecto y devuelvo al saldo), sino va a futuro y no importa.
				if(partidaAnio.getAnio().intValue() == ejercicioActual.getAnio().intValue() && partidaAnio.getImporte() > 0){

					V_SaldoAutorizadoLimitativa saldoAutorizadoLimitativa = V_SaldoAutorizadoLimitativaDAO.getInstance().getByConceptoLimitativa(ejercicioActual.getAnio(), partidaAnio);

					//Pedi la partida en la cuanta de Autorizado (DA) y me fijo su saldo, si por esas casualidades obtengo un null, es porque no exista la partida en la vista de saldo autorizado
//					if(saldoAutorizadoLimitativa != null){
//
//						//El saldo viene en negativo, si da mayor a 0 es porque me pase y no hay $$$$$
//						if(saldoAutorizadoLimitativa.getSaldo() + anioPartidaGwt.getImporte() > 0){
//							infoSaldoGwt.addPartidaSinSaldo(anioPartidaGwt);
//						}else{
//							//hay saldo y no tengo que hacer nada
//						}
//
//					}else{
//						infoSaldoGwt.addPartidaInexistente(anioPartidaGwt);
//					}
					controlarSaldo(infoSaldoGwt, partidaAnio, saldoAutorizadoLimitativa.getSaldo());

				}else{
					//partida con ejercicio furuto o tengo que devolver credito a la partida
				}
			}else{
				//si el importe de las partidas agrupadas es 0 no hago nada
			}

		}
		
		if (infoSaldoGwt.getListaPartidasInexistentes().size() > 0) {
			rta.setError("Hay Partidas que no están incluidas en la DA actual");
			rta.setErrorSaldo(infoSaldoGwt);
			throw new GwtSaldoParidaMovimientoException("ERROR GRAVE en consultaSaldo: Las partidas ingresadas no se encuentran en la DA actual => no es posibles realizar el movimiento contable: " + infoSaldoGwt.getMensaje()); 
		
		}else if(infoSaldoGwt.getListaPartidasSinSaldo().size() > 0){
			rta.setError("Partidas sin saldo en sus limitativas para poder afectar");
			rta.setErrorSaldo(infoSaldoGwt);
		
		}else{
			rta.setOk();
		}
		
		return rta;
	}
	
	/**
	 * 
	 * @param partidas
	 * @return
	 */
	private static List<PartidaAnio> agruparPartidasPorLimitativa(List<PartidaAnio> partidas) {

		List<PartidaAnio> listaPartidasAgrupadas = new ArrayList<PartidaAnio>();
		
		if(partidas.size() == 0) {
			return null;
		}

		boolean corte;
		for (PartidaAnio partida : partidas) {
			corte = false;

//			PartidaAnioGwt partidaLimitativa = new PartidaAnioGwt(AdapterPartida.get(partida));
			PartidaAnio partidaLimitativa = new PartidaAnio(partida);
//			partidaLimitativa.setConceptoGasto(AdapterConceptoGasto.get(ConceptoGastoDAO.getInstance().getUniqueBy(partida.getConceptoGasto().getPosicionLimitativa())));
			partidaLimitativa.setConceptoGasto(ConceptoGastoDAO.getInstance().getUniqueBy(partida.getConceptoGasto().getPosicionLimitativa()));
			
			for (int i = 0; i < listaPartidasAgrupadas.size(); i++) {
//				PartidaAnioGwt partidaAgrupada = listaPartidasAgrupadas.get(i);
				PartidaAnio partidaAgrupada = listaPartidasAgrupadas.get(i);

				if( partidaLimitativa.getConceptoGasto().getPosicion().equals(partidaAgrupada.getConceptoGasto().getPosicion()) &&
						partidaLimitativa.getAnio().intValue() == partidaAgrupada.getAnio().intValue() ){

					partidaAgrupada.setImporte(partidaAgrupada.getImporte()+partidaLimitativa.getImporte());
					i=listaPartidasAgrupadas.size();
					corte = true;
				}

			}
			if(!corte){
				listaPartidasAgrupadas.add(partidaLimitativa);
			}
		}

		return listaPartidasAgrupadas;
	}
	
	/**
	 * Valida el saldo afectado por limitativa
	 * @param afectacion
	 * @return
	 * @throws GwtSaldoParidaMovimientoException cuando hay partidas que no existen en la DA
	 */
	private static RespuestaMovimiento validarDesafectar(AfectacionProvisional afectacion) throws GwtSaldoParidaMovimientoException{
		RespuestaMovimiento rta = new RespuestaMovimiento();
		InfoSaldoPartidaGwt infoSaldoGwt = new InfoSaldoPartidaGwt();
		
		for (PartidaAnio partidaAnio : afectacion.getItemsPartidaAnio()) {
			
			controlarSaldo(infoSaldoGwt,partidaAnio, getSaldoAfectado(partidaAnio, afectacion));
			
		}
		
		if (infoSaldoGwt.getListaPartidasInexistentes().size() > 0) {
			rta.setError("Hay Partidas que no están afectadas bajo el expediente indicado");
			rta.setErrorSaldo(infoSaldoGwt);
			throw new GwtSaldoParidaMovimientoException("ERROR GRAVE en validarDesafectar: Las partidas ingresadas no se encuentran Afectadas bajo ese Expediente => no es posibles realizar el movimiento contable: " + infoSaldoGwt.getMensaje()); 
		
		}else if(infoSaldoGwt.getListaPartidasSinSaldo().size() > 0){
			rta.setError("Partidas sin saldo para poder registrar el movimiento");
			rta.setErrorSaldo(infoSaldoGwt);
			return rta;
		}else{
			rta.setOk();
			return rta;
		}
		
	}
	
	/**
	 * En función si la partida es del año actual o de año futuro, ve en donde tiene que consultar el saldo - recurso af actual o futuro?
	 * @param partidaAnio
	 * @param afectacion
	 * @return
	 */
	private static Double getSaldoAfectado(PartidaAnio partidaAnio,AfectacionProvisional afectacion ){
		Double saldoExpediente = null;
		Ejercicio ejercicioActual = afectacion.getEjercicio();
		
		//Discrimino si el afectado es del ejercicio actual o de un ejercicio futuro
		if(partidaAnio.getAnio().intValue() == ejercicioActual.getAnio().intValue()){
				saldoExpediente = V_SaldoAfectadoExpedienteDAO.getInstance().getSaldo(partidaAnio,afectacion.getExpediente(),CuentaDAO.PRE_RECUSOS_AFECTADOS);							
		
		}else if(partidaAnio.getAnio().intValue() > ejercicioActual.getAnio().intValue()){
				saldoExpediente = V_SaldoAfectadoExpedienteDAO.getInstance().getSaldo(partidaAnio,afectacion.getExpediente(),CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO);				
		}else{
			//ERROR: si el año de la partida es menor al ejercicio actual tiene que explotar.
			throw new GwtSaldoParidaMovimientoException("ERROR GRAVE en getSaldoAfectado: El año de la partida es menor al ejercicio actual");
		}
		
		return saldoExpediente;
	}
	
	
	private static void controlarSaldo(InfoSaldoPartidaGwt infoSaldoGwt, PartidaAnio partidaAnio, Double saldoExpediente) {
		if(saldoExpediente != null){
			
			//El saldo viene en negativo, si es mayor a 0 es porque no tengo saldo...
			if(saldoExpediente + partidaAnio.getImporte() > 0){
				
				infoSaldoGwt.addPartidaSinSaldo(AdapterPartida.get(partidaAnio));
				
			}else{
				//HAY SALDO 
			}
		}else{
			infoSaldoGwt.addPartidaInexistente(AdapterPartida.get(partidaAnio));
		}
	}
	
}
