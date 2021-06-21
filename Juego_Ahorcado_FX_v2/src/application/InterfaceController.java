package application;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class InterfaceController implements EventHandler<ActionEvent>{

	Partida partida;
	
	Hashtable<String, Button> botonesLetra;

	Image[] imagenesHorcas;

	int segundo = 0;

	Timer timer;
	
	// Variables fxml 
	@FXML
	GridPane teclado;
	@FXML
	Label palabraOculta;
	@FXML
	Label tiempo;
	@FXML
	Label aciertos;
	@FXML
	Label vidas;
	@FXML
	Label fallos;
	@FXML
	TextFlow textFlow;
	@FXML
	ImageView imageView;


	// Properties para hacer binding con las etiquetas de los contadores
	IntegerProperty prAciertos;
	IntegerProperty prVidas;
	IntegerProperty prFallos;
	BooleanProperty prPartidaEnCurso;
	StringProperty prTiempo;
	BooleanProperty prPerdida; // Para hacer binding con boton Nueva Partida.

	// Este metodo se ejecuta cuando están todos los elementos fxml inyectados

	public void initialize() {
		creaListenersBotonesLetras();
		cargaHorcas();
		creaBindins();
		partidaNueva();

	}

	private void creaBindins() {

		prAciertos = new SimpleIntegerProperty();
		aciertos.textProperty().bind(prAciertos.asString());

		prVidas = new SimpleIntegerProperty();
		vidas.textProperty().bind(prVidas.asString());

		prFallos = new SimpleIntegerProperty();
		fallos.textProperty().bind(prFallos.asString());

		prPartidaEnCurso = new SimpleBooleanProperty();
		botonesLetra.get("_Nueva Partida").disableProperty().bind(prPartidaEnCurso);
		
		prTiempo = new SimpleStringProperty();
		tiempo.textProperty().bind(prTiempo);

		prPerdida = new SimpleBooleanProperty();
		textFlow.visibleProperty().bind(prPerdida);
		palabraOculta.visibleProperty().bind(prPerdida.not());

	}

	private void creaListenersBotonesLetras() {
		botonesLetra = new Hashtable<>();
		System.out.println("Creando listeners botones...");
		for (Node n : teclado.getChildren()) {
			if (n instanceof Button) {
				Button b = (Button) n;
				b.setOnAction(this);
				botonesLetra.put(b.getText(), b);
			}
		}
		
		// Registra listener para recoger eventos de pulsación de teclado.
		teclado.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent e) {
				Button botonLetra;
				// Si el botón está en la hashtable de botones
				if ((botonLetra = botonesLetra.get(e.getText().toUpperCase())) != null)
					// Lanza el evento de botón pulsado. Esto es equivaliente a hacer clic en el
					// botón.
					botonLetra.fire();
				
			}
		});
		
		// Versión con expresiones lambda
		
//		teclado.setOnKeyPressed(e -> {
//			Button botonLetra;
//
//				if ((botonLetra = botonesLetra.get(e.getText().toUpperCase())) != null)
//					// Lanza el evento de botón pulsado. Esto es equivaliente a hacer clic en el
//					// botón.
//					botonLetra.fire();
//		});
	}

	private void partidaNueva() {
		segundo = 0;
		partida = new Partida();
		imageView.setImage(imagenesHorcas[0]);
		textFlow.getChildren().clear();
		palabraOculta.setText(partida.getPalabraCandidataEspaciada());
		prAciertos.setValue(partida.getAciertos());
		prVidas.setValue(partida.getVidas());
		prFallos.setValue(partida.getFallos());
		prPartidaEnCurso.setValue(true);
		prPerdida.setValue(false);
		prTiempo.setValue(formatoHora(segundo));
		actualizaBotonera();
		iniciaCronometro();

	}

	private void procesaBotonLetraPulsada(ActionEvent e) {
		String palEnCons;
		Button boton = (Button) e.getSource();

		// Si pulsado botón de nueva partida crea nueva partida
		// y sale del método
		if (boton.getText().equals("_Nueva Partida")) {
			partidaNueva();
			return;
		}

		boton.setDisable(true);

		// Si pulsado botón de una letra hace jugada

		if ((palEnCons = partida.hazJugada(boton.getText().charAt(0))) != null) {

			palabraOculta.setText(palEnCons); // Actualiza palabra en construccion
			prAciertos.set(partida.getAciertos()); // Act. contador aciertos

		} else {

			// Act. cont. fallos y vidas
			prFallos.setValue(partida.getFallos());
			prVidas.setValue(partida.getVidas());
			// Act. imagen horca
			imageView.setImage(imagenesHorcas[partida.getFallos()]);
		}

		// Verifica si la partida ha terminado
		int terminar = partida.terminada();
		String mensaje;

		if (terminar != Partida.NO_TERMINADA) { // Si partida terminada
			prPartidaEnCurso.setValue(false);
			actualizaBotonera(); // Actualiza botnes para nueva partida
			timer.cancel(); // Para reloj
			PintaPalabraV2();
			if (terminar == Partida.PERDIDA) {
				prPerdida.setValue(true);
				mensaje = "Oooooh, fallaste";
			} else
				mensaje = "¡Enhorabuena la has completado!";
			new Alert(AlertType.NONE, mensaje,ButtonType.OK).showAndWait();
			
		}
	}

	/**
	 * Pone enable/disable los botones de las letras en función de si la partida
	 * esta o no en curso. El botón Nueva Partida no se gestiona aquí ya que cambia
	 * mediante binding con la property prPartidaEnCurso. Los de las letras no lo
	 * gestionamos con binding ya que pueden estar abilitados o no mientras la
	 * partida esta en curso.
	 */

	private void actualizaBotonera() {
		for (String boton : botonesLetra.keySet()) {
			if (!boton.equals("_Nueva Partida"))
				botonesLetra.get(boton).setDisable(!prPartidaEnCurso.get());
		}
	}


	

	/**
	 * Pinta la palbra completa cuando se ha perdido la partida. Las letras no
	 * descubiertas se escriben en color rojo. Se utiliza el control TextFlow para
	 * ello.
	 */

	private void pintaPalabra() {

		String pOK = partida.getPalabraOculta();
		String pKO = partida.getPalabraCandidata();

		StringBuilder sb = new StringBuilder();

		// Clase interna local  para guardar el trozo de palabra y su color
		class TrozoPal {
			String trozoString;
			char color;

			public TrozoPal(String trozoString, char color) {
				this.trozoString = trozoString;
				this.color = color; // 'N' negro; 'R' rojo
			}
		}

		// Este array contendrá todos los trozos de palabras y el color con que
		// pintarlos: 'N': negro; 'R': rojo.
		
		// Ejemplo: Para la palabra CORDILLERA incompleta: C _ R _ _ L L E R _
		// trozos =  {{"C ",'N'}, {"O ",'R'}, {"R ",'N'}, {"D I ",'R'}, {"L L E R ",'N'},
		// {"A","R"}}
		ArrayList<TrozoPal> trozos = new ArrayList<>();

		// Extracción de los trozos

		int estado = pKO.charAt(0) == '_' ? 1 : 2;

		sb.append(pOK.charAt(0)).append(" ");

		int i = 1;

		while (i < pKO.length()) {
			if (estado == 1) {
				if (pKO.charAt(i) != '_') {				// estado = 1 y caracter != '_'
					trozos.add(new TrozoPal(sb.toString(), 'R'));
					sb.setLength(0);
					estado = 2;
				}
			} else if (pKO.charAt(i) == '_') {         // estado = 2 y caracter == '_'
				trozos.add(new TrozoPal(sb.toString(), 'N'));
				sb.setLength(0);
				estado = 1;
			}
			sb.append(pOK.charAt(i)).append(" ");
			i++;
		}
		
		// Cogemos último trozo metido en el stringBuilder sin el 
		// espacio del final.
		
		trozos.add(new TrozoPal(sb.toString().trim(), estado == 1 ? 'R' : 'N'));

		// Va dando el estilo y color a cada trozo y metiendolo en el TextFlow
		Text t;
		for (TrozoPal p : trozos) {
			t = new Text();
			t.setText(p.trozoString);
			t.setFill(p.color == 'R' ? Color.RED : Color.BLACK);
			t.setFont(Font.font("System", FontWeight.BOLD, 36));
			textFlow.getChildren().add(t);
		}
	}
	
	// Versión de PintaPalabra más simple de implementar pero genera un objeto Text
	// por cada letra. Además hace concatenaciones de strings
 
	void PintaPalabraV2() {
		String pOK = partida.getPalabraOculta();    // CORDILLERA
		String pKO = partida.getPalabraCandidata(); // C_R__LLER_
		Text t;
		int contLetras = pKO.length() - 1;
		for (int i = 0; i <= contLetras; i++) {
			String cOK = "" + pOK.charAt(i);
			String cKO = "" + pKO.charAt(i);
			
			if(i<contLetras) 
				 t  = new Text( cOK + " ");
			else 
				t = new Text(cOK) ;
			
			t.setFont(Font.font("System", FontWeight.BOLD, 36));
			t.setFill(cKO.equals(cOK) ? Color.BLACK : Color.RED) ;
			textFlow.getChildren().add(t);
		}		
	}

	
	void iniciaCronometro() {
		
		class TareaTimer extends TimerTask{

			@Override
			public void run() {
				// Desde un thread distinto al de fx no se puede actualizar un 
				//elemento del de fx. Para poder hacerlo hay que usar 
				//Platform.runLater()
				Platform.runLater(() -> {
					prTiempo.setValue(formatoHora(segundo++));
				});
				
			}
			
		}

		final int intervaloTimer = 1_000;

		timer = new Timer();
		// TimerTask: clase abstracta. Su método run es ejecutado por el Timer en el
		// intervalo especificado
		timer.schedule(new TareaTimer(), 0, intervaloTimer);
	}
	

	String formatoHora(long n) {
		long horas = 0, minutos = 0, segundos = 0;

		horas = n / 3600;
		minutos = (n % 3600) / 60;
		segundos = n % 60;

		return String.format("%02d : %02d : %02d", horas, minutos, segundos);

	}

	void cargaHorcas() {
		imagenesHorcas = new Image[Partida.TOTAL_VIDAS + 1];

		for (int i = 0; i < imagenesHorcas.length; i++) {

			imagenesHorcas[i] = new Image(getClass().getResource("/imagenes/ahorcado" + i + ".png").toString());

		}
	}

	@Override
	public void handle(ActionEvent event) {
		// TODO Auto-generated method stub
		procesaBotonLetraPulsada(event);
		
	}
	

	
}  // fin class
