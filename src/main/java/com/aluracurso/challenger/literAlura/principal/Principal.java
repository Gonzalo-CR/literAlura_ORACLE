package com.aluracurso.challenger.literAlura.principal;

import com.aluracurso.challenger.literAlura.model.Autor;
import com.aluracurso.challenger.literAlura.model.Datos;
import com.aluracurso.challenger.literAlura.model.DatosLibros;
import com.aluracurso.challenger.literAlura.model.DatosAutor;
import com.aluracurso.challenger.literAlura.model.Libro;
import com.aluracurso.challenger.literAlura.repository.LibroRepository;
import com.aluracurso.challenger.literAlura.repository.AutorRepository;
import com.aluracurso.challenger.literAlura.service.ConsumoAPI;
import com.aluracurso.challenger.literAlura.service.ConvierteDatos;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Service
public class Principal {

    private static final String URL_BASE = "https://gutendex.com/books/";
    private final ConsumoAPI consumoAPI;
    private final ConvierteDatos conversor;
    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final Scanner teclado = new Scanner(System.in);

    public Principal(ConsumoAPI consumoAPI, ConvierteDatos conversor, LibroRepository libroRepository, AutorRepository autorRepository) {
        this.consumoAPI = consumoAPI;
        this.conversor = conversor;
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                 \n   **LiterAlura**
                Elija una opción:
                1 - Buscar libro por título (registra la búsqueda)
                2 - Buscar Autores
                3 - Buscar libros por autor y año
                4 - Buscar libros por idioma disponible
                5 - Ver todos los libros disponibles
                6 - Ver Top 10 de libros más descargados
                0 - Salir
                """;
            System.out.println(menu);

            try {
                if (teclado.hasNextInt()) {
                    opcion = teclado.nextInt();
                    teclado.nextLine(); // Limpiar el buffer de entrada

                    switch (opcion) {
                        case 1 -> buscarLibro();
                        case 2 -> buscarAutoresRegistrados();
                        case 3 -> buscarLibroPorAutorYFecha();
                        case 4 -> buscarLibroPorIdioma();
                        case 5 -> listarLibrosDisponibles();
                        case 6 -> mostrarTopLibros();
                        case 0 -> {
                            System.out.println("Cerrando la aplicación...");
                            teclado.close(); // Cierra el Scanner
                            System.exit(0);  // Finaliza el programa
                        }
                        default -> System.out.println("Opción inválida.");
                    }
                } else {
                    System.out.println("Entrada no válida, por favor ingresa un número entero.");
                    teclado.nextLine(); // Limpiar el buffer
                }
            } catch (Exception e) {
                System.out.println("Error inesperado: " + e.getMessage());
            }
        }
    }

    @Transactional
    private void buscarLibro() {
        System.out.println("Ingrese una palabra clave para buscar el libro:");
        String palabraClave = teclado.nextLine().trim().toLowerCase();

        // Buscar en la base de datos usando LIKE
        List<Libro> candidatos = libroRepository.findLibrosCandidatosPorTitulo(palabraClave);

        // Filtrar por palabra completa
        List<Libro> librosFiltrados = candidatos.stream()
                .filter(libro -> contienePalabraCompleta(libro.getTitulo(), palabraClave))
                .toList();

        if (!librosFiltrados.isEmpty()) {
            System.out.println("Libros encontrados en la base de datos:");
            librosFiltrados.forEach(this::mostrarInformacionLibro);
        } else {
            System.out.println("No se encontró ningún libro en la base de datos. Buscando en la API...");

            // Buscar en la API
            Datos datosBusqueda = obtenerDatosLibros(URL_BASE + "?search=" + palabraClave.replace(" ", "+"));
            Optional<DatosLibros> libroAPI = datosBusqueda.resultados().stream()
                    .filter(libro -> contienePalabraCompleta(libro.titulo().toLowerCase(), palabraClave))
                    .findFirst();

            libroAPI.ifPresentOrElse(datosLibro -> {
                System.out.println("\nLibro encontrado en la API:");
                System.out.println("Título: " + datosLibro.titulo());
                registrarLibroDesdeAPI(datosLibro);
            }, () -> System.out.println("No se encontró ningún libro con esa palabra clave."));
        }
    }


    private void registrarLibroDesdeAPI(DatosLibros datosLibro) {
        Libro nuevoLibro = new Libro(datosLibro);
        List<Autor> autores = datosLibro.autor().stream()
                .map(datosAutor -> autorRepository.findByNombre(datosAutor.nombre().toLowerCase().trim())
                        .orElseGet(() -> {
                            Autor autor = new Autor(datosAutor.nombre().toLowerCase().trim(),
                                    datosAutor.fechaDeNacimiento(),
                                    datosAutor.fechaDeFallecimiento());
                            autorRepository.save(autor);
                            return autor;
                        })).toList();

        nuevoLibro.setAutores(autores);

        if (!libroRepository.existsByTituloIgnoreCase(nuevoLibro.getTitulo())) {
            try {
                libroRepository.save(nuevoLibro);
                System.out.println("El libro ha sido registrado exitosamente.");
            } catch (DataIntegrityViolationException e) {
                System.out.println("Error al intentar registrar el libro: " + nuevoLibro.getTitulo());
            }
        }
    }

    @Transactional
    private void buscarAutoresRegistrados() {
        System.out.println("Ingrese una palabra clave para buscar el autor:");
        String palabraClave = teclado.nextLine().trim().toLowerCase();

        // Buscar en la base de datos con LIKE
        List<Autor> candidatos = autorRepository.findByNombreCandidato(palabraClave);

        // Validar palabras completas dentro del nombre
        List<Autor> autoresFiltrados = candidatos.stream()
                .filter(autor -> contienePalabraCompletaEnNombre(autor.getNombre(), palabraClave))
                .toList();

        if (autoresFiltrados.isEmpty()) {
            System.out.println("No se encontró ningún autor con esa palabra clave.");
        } else {
            System.out.println("Autores encontrados:");
            autoresFiltrados.forEach(this::mostrarDatosAutor);
        }
    }


    private boolean contienePalabraCompleta(String texto, String palabraClave) {
        String[] palabras = texto.split("\\s+");
        for (String palabra : palabras) {
            if (palabra.equalsIgnoreCase(palabraClave)) {
                return true;
            }
        }
        return false;
    }

    private boolean contienePalabraCompletaEnNombre(String nombreCompleto, String palabraClave) {
        // Separar por espacios o comas para cubrir nombres completos como "Melville, Herman"
        String[] palabras = nombreCompleto.split("[\\s,]+");
        for (String palabra : palabras) {
            if (palabra.equalsIgnoreCase(palabraClave)) {
                return true;
            }
        }
        return false;
    }


    private Datos obtenerDatosLibros(String url) {
        var json = consumoAPI.obtenerDatos(url);
        return conversor.obtenerDatos(json, Datos.class);
    }

    private void mostrarInformacionLibro(Libro libro) {
        System.out.println("\n****** Información del Libro ******");
        System.out.println("Título: " + libro.getTitulo());
        System.out.println("Idiomas: " + String.join(", ", libro.getIdiomas()));
        System.out.println("Descargas: " + libro.getNumeroDeDescargas());
        System.out.println("Autor(es): " + libro.getAutores().stream()
                .map(Autor::getNombre)
                .collect(Collectors.joining(", ")));
        System.out.println("***********************************\n");
    }

    private void mostrarDatosAutor(Autor autor) {
        Hibernate.initialize(autor.getBibliografias());
        System.out.println("\nAutor encontrado:");
        System.out.println("Nombre: " + autor.getNombre());
        System.out.println("Fecha de Nacimiento: " + autor.getFechaDeNacimiento());
        System.out.println("Fecha de Fallecimiento: " + autor.getFechaDeFallecimiento());
        System.out.println("Libros: " + autor.getLibros().stream()
                .map(Libro::getTitulo)
                .collect(Collectors.joining(", ")));
        System.out.println("*********************\n");
    }


    private void mostrarTopLibros() {
        var datos = obtenerDatosLibros(URL_BASE);
        System.out.println("  Top 10 de libros más descargados:\n");
        var librosOrdenados = datos.resultados().stream()
                .sorted(Comparator.comparing(DatosLibros::numeroDeDescargas).reversed())
                .limit(10)
                .toList();

        for (int i = 0; i < librosOrdenados.size(); i++) {
            var libro = librosOrdenados.get(i);
            System.out.printf("%d. %s\n", i + 1, libro.titulo().toUpperCase());
        }
    }

    private void listarLibrosDisponibles() {
        var libros = libroRepository.findAll();

        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
            return;
        }

        System.out.println("Lista de libros registrados:");
        libros.forEach(libro -> {
            System.out.println("****** Libro *******");
            System.out.println("Título: " + libro.getTitulo());
            System.out.println("Autor(es): " + libro.getAutores().stream()
                    .map(Autor::getNombre)
                    .collect(Collectors.joining(", ")));
            System.out.println("Idiomas: " + String.join(", ", libro.getIdiomas()));
            System.out.println("Descargas: " + libro.getNumeroDeDescargas());
            System.out.println("*********************\n");
        });
    }

    private void buscarLibroPorIdioma() {
        var idiomasDisponibles = libroRepository.findDistinctIdiomas();

        if (idiomasDisponibles.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos para obtener idiomas.");
            return;
        }

        while (true) {
            System.out.println("Idiomas disponibles:");
            for (int i = 0; i < idiomasDisponibles.size(); i++) {
                System.out.printf("%d - %s\n", i + 1, idiomasDisponibles.get(i));
            }

            System.out.println("Seleccione un idioma (ingrese el número correspondiente):");
            try {
                int opcion = teclado.nextInt();
                teclado.nextLine(); // Limpiar buffer

                if (opcion < 1 || opcion > idiomasDisponibles.size()) {
                    System.out.println("Opción inválida. Por favor, seleccione un número válido.");
                    continue;
                }

                String idiomaSeleccionado = idiomasDisponibles.get(opcion - 1);
                var librosPorIdioma = libroRepository.findLibrosPorIdioma(idiomaSeleccionado);

                if (librosPorIdioma.isEmpty()) {
                    System.out.println("No se encontraron libros en el idioma seleccionado.");
                } else {
                    System.out.println("Libros disponibles en " + idiomaSeleccionado + ":");
                    librosPorIdioma.forEach(libro -> System.out.println(" - " + libro.getTitulo()));
                }
                break;
            } catch (Exception e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número válido.");
                teclado.nextLine(); // Limpiar buffer en caso de error
            }
        }
    }


    @Transactional
    private void buscarLibroPorAutorYFecha() {
        System.out.println("Ingrese el año para filtrar autores:");
        try {
            int anoSeleccionado = teclado.nextInt();
            teclado.nextLine();

            var autoresVivos = autorRepository.findAutoresVivosEnAno(anoSeleccionado);

            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos en el año " + anoSeleccionado + ".");
                return;
            }

            System.out.println("Autores vivos en el año " + anoSeleccionado + ":");
            for (int i = 0; i < autoresVivos.size(); i++) {
                var autor = autoresVivos.get(i);
                System.out.printf("%d - %s\n", i + 1, autor.getNombre());
            }

            System.out.println("Seleccione un autor (ingrese el número correspondiente):");
            int opcion = teclado.nextInt();
            teclado.nextLine();

            if (opcion < 1 || opcion > autoresVivos.size()) {
                System.out.println("Opción inválida.");
                return;
            }

            var autorSeleccionado = autoresVivos.get(opcion - 1);
            mostrarDatosAutor(autorSeleccionado);
        } catch (Exception e) {
            System.out.println("Entrada no válida. Por favor, ingrese un número válido.");
            teclado.nextLine(); // Limpiar buffer
        }
    }
}
