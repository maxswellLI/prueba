package movimientos.old;

import java.util.Date;
import java.util.List;
import java.util.Set;

import contable.server.constants.MovimientoConstants;
import contable.server.dao.CuentaDAO;
import contable.server.domain.AfectacionProvisional;
import contable.server.domain.AfectacionProvisionalDet;
import contable.server.domain.AjustePartida;
import contable.server.domain.CasoDeUsoMovimiento;
import contable.server.domain.CompromisoContraidoDet;
import contable.server.domain.PartidaAnio;
import contable.server.domain.CompromisoContraido;
import contable.server.domain.Cuenta;
import contable.server.domain.DecisionAdministrativa;
import contable.server.domain.Movimiento;
import contable.server.domain.MovimientoDet;
import contable.server.domain.MovimientoDetCuentaPresupuesto;
import contable.server.exception.GwtMovimientoException;

/**
 * 
 * @author Max Ariosti
 *
 */
@Deprecated
public abstract class MovimientosUtils {
	

	
	
//	/**
//	 * Crea el movimiento correspondiente a un Ajuste Presupuesatario en donde los items del Movimiento involucra las cuentas Credito_Ajuste y Gasto_Autorizado 
//	 * @param ajuste
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearMovimientoAjustePartida(AjustePartida ajuste) throws GwtMovimientoException {
//		Movimiento movimiento = new Movimiento();
//		movimiento.setEjercicio(ajuste.getEjercicio());
//		movimiento.setFecha(new Date());
//		movimiento.setNombre("Ajuste de Partida");
//		
//		//ver si luego no es necesario un tipo de movimiento
//		
//		Set<MovimientoDet> movimientos = crearMovimientosPresupuestarios(movimiento,ajuste.getPartidasAnio(), CuentaDAO.PRE_CREDITO_AJUSTES, CuentaDAO.PRE_GASTOS_AUTORIZADO);
//		movimiento.setMovimientos(movimientos);		
//		
//		return movimiento;
//	}
//
//	/**
//	 * Revertir el movimiento correspondiente a un <b>AJUSTE PRESUPUESTARIO</b> en donde los items del Movimiento involucra las cuentas <i>Gasto_Autorizado y Credito_Ajuste</i>  
//	 * @param ajuste
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearMovimientoRevertirConformeAjuste(AjustePartida ajuste) {
//		Movimiento movimiento = new Movimiento();
//		movimiento.setEjercicio(ajuste.getEjercicio());
//		movimiento.setFecha(new Date());
//		movimiento.setNombre("Revertir Conforme de Ajuste Presupuestario");
//		
//		Set<MovimientoDet> movimientos = crearMovimientosPresupuestarios(movimiento,ajuste.getPartidasAnio(), CuentaDAO.PRE_GASTOS_AUTORIZADO, CuentaDAO.PRE_CREDITO_AJUSTES);
//		movimiento.setMovimientos(movimientos);		
//		
//		
//		return movimiento;
//	}
//	
//	/**
//	 * Crea un movimiento para la cuenta de <b>DECISION ADMINISTRIVA</b> pasando del credito incial al gasto autorizado
//	 * @param da
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearMovimientoDecisionAdministrativa(DecisionAdministrativa da) throws GwtMovimientoException {
//		Movimiento movimiento = new Movimiento();
//		movimiento.setEjercicio(da.getEjercicio());
//		movimiento.setFecha(new Date());
//		movimiento.setNombre("Decision Administrativa");
//		
//		//ver si luego no es necesario un tipo de movimiento
//
//		Set<MovimientoDet> movimientos = crearMovimientosPresupuestarios(movimiento,da.getPartidasAnio(), CuentaDAO.PRE_CREDITO_INICIAL, CuentaDAO.PRE_GASTOS_AUTORIZADO);
//		movimiento.setMovimientos(movimientos);		
//
//		return movimiento;
//	}
//	
//	/**
//	 * Crea un movimiento para revertir el conforme de una <b>DECISION ADMINISTRIVA</b> devolviendo crédito a la cuenta de credito inicial 
//	 * @param da
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearMovimientoRevertirConformeDA(DecisionAdministrativa da) throws GwtMovimientoException {
//	Movimiento movimiento = new Movimiento();
//	movimiento.setEjercicio(da.getEjercicio());
//	movimiento.setFecha(new Date());
//	movimiento.setNombre("Revertir Conforme de DA");
//	
//	Set<MovimientoDet> movimientos = crearMovimientosPresupuestarios(movimiento,da.getPartidasAnio(), CuentaDAO.PRE_GASTOS_AUTORIZADO, CuentaDAO.PRE_CREDITO_INICIAL);
//	movimiento.setMovimientos(movimientos);		
//	
//	return movimiento;
//	}
	

	/**
	 * Crea los items relacionado con partidas presupuestarias para un determinado movimiento contable.
	 * @param movimiento
	 * @param partidas
	 * @param cuentaOrigen
	 * @param cuentaDestino
	 * @return
	 */
	protected static Set<MovimientoDet> crearMovimientosPresupuestarios( Movimiento movimiento, List<PartidaAnio> partidas, Cuenta cuentaOrigen, Cuenta cuentaDestino) {
	//veo cuanto va acumulando, despues borrarlo.
		Set<MovimientoDet> movimientos = movimiento.getMovimientos();
		
		Double acumPos = new Double(0);
		Double acumNeg = new Double(0);
		
		for (PartidaAnio partida : partidas) {
			movimientos.add(crearMovDetCuentaPresupuestaria(cuentaOrigen,partida, MovimientoConstants.POSITIVO));
			acumPos += (MovimientoConstants.POSITIVO*partida.getImporte());
		}

		for (PartidaAnio partida : partidas) {
			movimientos.add(crearMovDetCuentaPresupuestaria(cuentaDestino,partida, MovimientoConstants.NEGATIVO));
			acumNeg += (MovimientoConstants.NEGATIVO*partida.getImporte());
		}

		if((acumPos + acumNeg) != 0) 
			throw new GwtMovimientoException("crearMovimientoAjustePartida - La suma de los importes no es cero es: " + (acumPos + acumNeg));
		
		return movimientos;
	}

	/**
	 * Crea un solo item de cuenta presupuestaria, osea, con partida
	 * @param cuenta
	 * @param partida
	 * @param signo me indica si el importe es positivo o negativo
	 * @return
	 */
	protected static MovimientoDetCuentaPresupuesto crearMovDetCuentaPresupuestaria(Cuenta cuenta,PartidaAnio partida,double signo) {
		MovimientoDetCuentaPresupuesto movCuenta = new MovimientoDetCuentaPresupuesto();
		
		//atributos propios de movDet
		movCuenta.setCuenta(cuenta);
		movCuenta.setImporte(partida.getImporte()*signo);
		
		//atributos propios de mov cuenta
		movCuenta.setPartida(partida);
		movCuenta.setAnio(partida.getAnio());
		
		return movCuenta;
	}


//	/**
//	 * Crea los movimientos necesarios para la <B>COMPROMISO CONTRAIDO</b>. Se encarga de tomar los items del compromiso 
//	 * dividiendolos en actuales y futuros de manera que el movimiento se registre en <i><b>recurso_comprometido</i></b>  o <i><b>recurso_comprometido_a_futuro</i></b>  
//	 * @param comprimiso
//	 * @param casoDeUso
//	 * @return movimiento
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearCompromisoContraido(CompromisoContraido comprimiso, CasoDeUsoMovimiento casoDeUso)  throws GwtMovimientoException {
//		CompromisoContraido compromisoActual = new CompromisoContraido(comprimiso);
//		CompromisoContraido compromisoFuturo = new CompromisoContraido(comprimiso);
//		
//		Movimiento movimiento = new Movimiento(comprimiso.getEjercicio(), casoDeUso);
//		
//		//separamos los items que correspondan al ejercicio en curso y cuales no
//		separarCompromisoPorEjercicio(comprimiso, compromisoActual, compromisoFuturo);
//		
//		Set<MovimientoDet> movimientosActuales = crearMovimientosPresupuestarios(movimiento,compromisoActual,CuentaDAO.PRE_RECUSOS_AFECTADOS, CuentaDAO.PRE_RECUSOS_COMPROMETIDOS);
//		Set<MovimientoDet> movimientosFuturos = crearMovimientosPresupuestarios(movimiento,compromisoFuturo,CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO, CuentaDAO.PRE_RECUSOS_COMPROMETIDOS_A_FUTURO);
//		
//		movimientosActuales.addAll(movimientosFuturos);
//		movimiento.setMovimientos(movimientosActuales);		
//		
//		return movimiento;
//	}
//
//	/**
//	 * Crea los movimientos necesarios para la <B>DESHACER COMPROMISO CONTRAIDO</b>. Se encarga de tomar los items del compromiso 
//	 * dividiendolos en actuales y futuros de manera que el movimiento se registre en <i><b>recurso_afectado</i></b>  o <i><b>recurso_afectado_a_futuro</i></b>  
//	 * @param comprimiso
//	 * @param casoDeUso
//	 * @return movimiento
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento deshacerCompromisoContraido(CompromisoContraido comprimiso, CasoDeUsoMovimiento casoDeUso)  throws GwtMovimientoException {
//		CompromisoContraido compromisoActual = new CompromisoContraido(comprimiso);
//		CompromisoContraido compromisoFuturo = new CompromisoContraido(comprimiso);
//		
//		Movimiento movimiento = new Movimiento(comprimiso.getEjercicio(), casoDeUso);
//		
//		//separamos los items que correspondan al ejercicio en curso y cuales no
//		separarCompromisoPorEjercicio(comprimiso, compromisoActual, compromisoFuturo);
//		
//		Set<MovimientoDet> movimientosActuales = crearMovimientosPresupuestarios(movimiento,compromisoActual, CuentaDAO.PRE_RECUSOS_COMPROMETIDOS, CuentaDAO.PRE_RECUSOS_AFECTADOS);
//		Set<MovimientoDet> movimientosFuturos = crearMovimientosPresupuestarios(movimiento,compromisoFuturo, CuentaDAO.PRE_RECUSOS_COMPROMETIDOS_A_FUTURO, CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO);
//		
//		movimientosActuales.addAll(movimientosFuturos);
//		movimiento.setMovimientos(movimientosActuales);		
//		
//		return movimiento;
//	}
//	private static Set<MovimientoDet> crearMovimientosPresupuestarios( Movimiento movimiento, CompromisoContraido compromiso, Cuenta cuentaOrigen, Cuenta cuentaDestino) {
//		//veo cuanto va acumulando, despues borrarlo.
//		Set<MovimientoDet> movimientos = movimiento.getMovimientos();
//		
//		Double acumPos = new Double(0);
//		Double acumNeg = new Double(0);
//		for (CompromisoContraidoDet item : compromiso.getItems()) {
//			
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = crearMovDetCuentaPresupuestaria(cuentaOrigen,item.getPartidaAnio(), MovimientoConstants.POSITIVO);
//			movDetCuentaPresupuestaria.setExpediente(compromiso.getExpediente());
//			movDetCuentaPresupuestaria.setProveedor(compromiso.getProveedor());
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumPos += (MovimientoConstants.POSITIVO*item.getImporte());
//		}
//
//		for (CompromisoContraidoDet item : compromiso.getItems()) {
//
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = crearMovDetCuentaPresupuestaria(cuentaDestino,item.getPartidaAnio(), MovimientoConstants.NEGATIVO);
//			movDetCuentaPresupuestaria.setExpediente(compromiso.getExpediente());			
//			movDetCuentaPresupuestaria.setProveedor(compromiso.getProveedor());
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumNeg += (MovimientoConstants.NEGATIVO*item.getImporte());
//		}
//
//		
//		if((acumPos + acumNeg) != 0) 
//			throw new GwtMovimientoException("crearMovimientosPresupuestarios - Compromiso - La suma de los importes no es cero es: " + (acumPos + acumNeg));
//		
//		return movimientos;
//	}

//	/**
//	 * Crea los movimientos necesarios para la <B>AFECTACION PROVISIONAL</b>. Se encarga de tomar los items de la afectacion 
//	 * dividiendolos en actuales y futuros de manera que el movimiento se registre en <i><b>recurso_afectado</i></b>  o <i><b>recurso_afectado_a_futuro</i></b>  
//	 * @param afectacion
//	 * @param casoDeUso
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento crearAfectacionProvisional(AfectacionProvisional afectacion, CasoDeUsoMovimiento casoDeUso)  throws GwtMovimientoException {
//		AfectacionProvisional afectacionActual = new AfectacionProvisional(afectacion);
//		AfectacionProvisional afectacionFuturo = new AfectacionProvisional(afectacion);
//		
//		Movimiento movimiento = new Movimiento(afectacion.getEjercicio(), casoDeUso);
//
//		//separamos los items que correspondan al ejercicio en curso y cuales no
//		separarAfectacionPorEjercicio(afectacion, afectacionActual, afectacionFuturo);
//		
//		Set<MovimientoDet> movimientosActuales = crearMovimientosPresupuestarios(movimiento,afectacionActual,CuentaDAO.PRE_GASTOS_AUTORIZADO, CuentaDAO.PRE_RECUSOS_AFECTADOS);
//		Set<MovimientoDet> movimientosFuturos = crearMovimientosPresupuestarios(movimiento,afectacionFuturo,CuentaDAO.PRE_CREDITO_INICIAL_A_FUTURO, CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO);
//		
//		movimientosActuales.addAll(movimientosFuturos);
//		movimiento.setMovimientos(movimientosActuales);		
//		
//		return movimiento;
//	}
//	
//	/**
//	 * Crea los movimientos necesarios para la <b>DESAFECTACION PROVISIONAL</b>. Se encarga de tomar los items de la afectacion 
//	 * dividiendolos en actuales y futuros de manera que el movimiento se registre en <b><i>gasto_autorizado</i></b> o <b><i>credito_inicial_a_futuro</i></b> 
//	 * @param afectacion
//	 * @param casoDeUso
//	 * @return
//	 * @throws GwtMovimientoException
//	 */
//	public static Movimiento deshacerAfectacionProvisional(AfectacionProvisional afectacion, CasoDeUsoMovimiento casoDeUso)  throws GwtMovimientoException {
//		Movimiento movimiento = new Movimiento(afectacion.getEjercicio(), casoDeUso);
//
//		AfectacionProvisional afectacionActual = new AfectacionProvisional(afectacion);
//		AfectacionProvisional afectacionFuturo = new AfectacionProvisional(afectacion);
//		
//		separarAfectacionPorEjercicio(afectacion, afectacionActual, afectacionFuturo);
//
//		Set<MovimientoDet> movimientosActuales = crearMovimientosPresupuestarios(movimiento,afectacionActual, CuentaDAO.PRE_RECUSOS_AFECTADOS,CuentaDAO.PRE_GASTOS_AUTORIZADO);
//		Set<MovimientoDet> movimientosFuturos = crearMovimientosPresupuestarios(movimiento,afectacionFuturo, CuentaDAO.PRE_RECUSOS_AFECTADOS_A_FUTURO,CuentaDAO.PRE_CREDITO_INICIAL_A_FUTURO);
//		
//		movimientosActuales.addAll(movimientosFuturos);
//		movimiento.setMovimientos(movimientosActuales);	
//		
//		return movimiento;
//	}
	
//	/**
//	 * Permite que a partir de un item de afectación provisional determinemos si corresponde al ejercicio actual o uno futuro.
//	 * @param afectacion
//	 * @param afectacionActual
//	 * @param afectacionFuturo
//	 */
//	private static void separarAfectacionPorEjercicio(AfectacionProvisional afectacion, AfectacionProvisional afectacionActual, AfectacionProvisional afectacionFuturo) {
//		for (AfectacionProvisionalDet item : afectacion.getItems()) {
//			
//			if(item.getAnio().intValue() == afectacion.getEjercicio().getAnio().intValue()){
//				afectacionActual.addItem(item);
//			}else if(item.getAnio().intValue() > afectacion.getEjercicio().getAnio().intValue()){
//				afectacionFuturo.addItem(item);
//			}else{
//				throw new GwtMovimientoException("separarAfectacionPorEjercicio - año del item menor al de la afectacion");
//			}
//		}
//	}
	
//	private static void separarCompromisoPorEjercicio(CompromisoContraido compromiso, CompromisoContraido compromisoActual, CompromisoContraido compromisoFuturo) {
//		for (CompromisoContraidoDet item : compromiso.getItems()) {
//			
//			if(item.getAnio().intValue() == compromiso.getEjercicio().getAnio().intValue()){
//				compromisoActual.addItem(item);
//			}else if(item.getAnio().intValue() > compromiso.getEjercicio().getAnio().intValue()){
//				compromisoFuturo.addItem(item);
//			}else{
//				throw new GwtMovimientoException("separarCompromisoPorEjercicio - año del item menor al del compromiso");
//			}
//		}
//	}

//	/**
//	 * 
//	 * @param movimiento
//	 * @param afectacion
//	 * @param cuentaOrigen
//	 * @param cuentaDestino
//	 * @return
//	 */
//	private static Set<MovimientoDet> crearMovimientosPresupuestarios( Movimiento movimiento, AfectacionProvisional afectacion, Cuenta cuentaOrigen, Cuenta cuentaDestino) {
//		//veo cuanto va acumulando, despues borrarlo.
//		Set<MovimientoDet> movimientos = movimiento.getMovimientos();
//		
//		Double acumPos = new Double(0);
//		Double acumNeg = new Double(0);
//		for (AfectacionProvisionalDet item : afectacion.getItems()) {
//			
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = crearMovDetCuentaPresupuestaria(cuentaOrigen,item.getPartidaAnio(), MovimientoConstants.POSITIVO);
//			movDetCuentaPresupuestaria.setExpediente(afectacion.getExpediente());
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumPos += (MovimientoConstants.POSITIVO*item.getImporte());
//		}
//
//		for (AfectacionProvisionalDet item : afectacion.getItems()) {
//
//			MovimientoDetCuentaPresupuesto movDetCuentaPresupuestaria = crearMovDetCuentaPresupuestaria(cuentaDestino,item.getPartidaAnio(), MovimientoConstants.NEGATIVO);
//			movDetCuentaPresupuestaria.setExpediente(afectacion.getExpediente());			
//			movimientos.add(movDetCuentaPresupuestaria);
//			acumNeg += (MovimientoConstants.NEGATIVO*item.getImporte());
//		}
//
//		
//		if((acumPos + acumNeg) != 0) 
//			throw new GwtMovimientoException("crearMovimientosPresupuestarios - Afectacion - La suma de los importes no es cero es: " + (acumPos + acumNeg));
//		
//		return movimientos;
//	}
	



}
