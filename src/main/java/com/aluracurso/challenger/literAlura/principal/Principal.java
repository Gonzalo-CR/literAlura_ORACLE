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
import org.antlr.v4.runtime.InputMismatchException;
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
                boolean validOption = false;
                while (!validOption) {
                    if (teclado.hasNextInt()) {
                        opcion = teclado.nextInt();
                        teclado.nextLine(); // Limpiar el buffer de entrada
                        validOption = true;
                    } else {
                        System.out.println("Entrada no válida, por favor ingresa un número entero.");
                        teclado.nextLine(); // Limpiar el buffer
                    }
                }

                switch (opcion) {
                    case 1:
                        buscarLibro();
                        break;
                    case 2:
                        buscarAutoresRegistrados();
                        break;
                    case 3:
                        buscarLibroPorAutorYFecha();
                        break;
                    case 4:
                        buscarLibroPorIdioma();
                        break;
                    case 5:
                        listarLibrosDisponibles();
                        break;
                    case 6:
                        mostrarTopLibros();
                        break;
                    case 0:
                        System.out.println("Cerrando la aplicación...");
                        teclado.close(); // Cierra el Scanner
                        System.exit(0);  // Finaliza el programa
                        break;
                    default:
                        System.out.println("Opción inválida");
                }
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida, por favor ingresa un número entero.");
                teclado.nextLine(); // Limpiar el buffer para la próxima entrada
            }
        }
    }


    private Datos obtenerDatosLibros(String url) {
        var json = consumoAPI.obtenerDatos(url);
        return conversor.obtenerDatos(json, Datos.class);
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

    @Transactional
    private void buscarLibro() {
        System.out.println("Ingrese una palabra clave para buscar el libro:");
        var palabraClave = teclado.nextLine();

        // Buscar el libro en la base de datos solo por el título (ignorando mayúsculas/minúsculas)
        var libroExistente = libroRepository.findFirstByTituloContainingIgnoreCase(palabraClave);

        if (libroExistente.isPresent()) {
            // Si el libro ya existe, mostrar información del libro
            var libro = libroExistente.get();
            System.out.println("\nEl libro ya está registrado en la base de datos.\n");
            System.out.println("Título: " + libro.getTitulo());
            System.out.println("Idiomas: " + String.join(", ", libro.getIdiomas()));
            System.out.println("Descargas: " + libro.getNumeroDeDescargas());
            System.out.println("Autores: " + libro.getAutores().stream()
                    .map(Autor::getNombre)
                    .collect(Collectors.joining(", ")));
        } else {
            // Si el libro no existe, buscarlo en la API
            var datosBusqueda = obtenerDatosLibros(URL_BASE + "?search=" + palabraClave.replace(" ", "+"));

            Optional<DatosLibros> libroBuscado = datosBusqueda.resultados().stream().findFirst();

            libroBuscado.ifPresentOrElse(datosLibro -> {
                System.out.println("\nLibro encontrado en la API: \n");
                System.out.println("Título: " + datosLibro.titulo());
                System.out.println("Idiomas: " + String.join(", ", datosLibro.idiomas()));
                System.out.println("Descargas: " + datosLibro.numeroDeDescargas());
                System.out.println("Autores: " + datosLibro.autor().stream()
                        .map(DatosAutor::nombre)
                        .collect(Collectors.joining(", ")));

                // Crear un nuevo objeto Libro a partir de los datos obtenidos de la API
                var nuevoLibro = new Libro(datosLibro);

                // Buscar o crear los autores relacionados con el libro
                var autores = datosLibro.autor().stream()
                        .map(datosAutor -> autorRepository.findByNombre(datosAutor.nombre().toLowerCase().trim())
                                .orElseGet(() -> {
                                    var autor = new Autor(datosAutor.nombre().toLowerCase().trim(),
                                            datosAutor.fechaDeNacimiento(),
                                            datosAutor.fechaDeFallecimiento());
                                    autorRepository.save(autor);
                                    return autor;
                                })).toList();

                // Asignar los autores al libro
                nuevoLibro.setAutores(autores);

                // Verificar si el libro ya existe en la base de datos antes de intentar guardarlo
                if (libroRepository.existsByTituloIgnoreCase(nuevoLibro.getTitulo())) {
                    System.out.println("El libro ya existe en la base de datos: " + nuevoLibro.getTitulo());
                } else {
                    try {
                        // Intentar guardar el libro en la base de datos
                        libroRepository.save(nuevoLibro);
                        System.out.println("El libro ha sido registrado exitosamente en la base de datos.");
                    } catch (DataIntegrityViolationException e) {
                        // En caso de que haya un error de integridad (aunque no debería llegar aquí por la verificación previa)
                        System.out.println("Error al intentar registrar el libro. Puede que ya exista en la base de datos: " + nuevoLibro.getTitulo());
                    }
                }
            }, () -> System.out.println("\nNo se encontró ningún libro con esa palabra clave en la API."));
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
                    System.out.println("Opción inválida. Por favor, seleccione un número entre 1 y " + idiomasDisponibles.size() + ".");
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
            } catch (InputMismatchException e) {
                System.out.println("Entrada no válida. Por favor, ingrese un número válido.");
                teclado.nextLine(); // Limpiar buffer en caso de error
            }
        }
    }



    @Transactional
    private void buscarAutoresRegistrados() {
        System.out.println("Ingrese una palabra clave para buscar el autor:");
        var palabraClave = teclado.nextLine();

        // Obtener todos los autores que coincidan con la palabra clave
        List<Autor> autores = autorRepository.findByNombreWithLibros(palabraClave);

        // Verificar si se encontraron autores
        if (autores.isEmpty()) {
            System.out.println("\nNo se encontró ningún autor con esa palabra clave.");
            return;
        }

        // Si hay más de un autor, mostrar la lista y permitir que el usuario elija uno
        if (autores.size() > 1) {
            System.out.println("\nSe encontraron varios autores. Elija uno:");
            for (int i = 0; i < autores.size(); i++) {
                System.out.println((i + 1) + ". " + autores.get(i).getNombre());
            }

            // El usuario elige un autor
            System.out.print("\nIngrese el número del autor: ");
            int opcion = Integer.parseInt(teclado.nextLine()) - 1;

            // Validar que la opción es válida
            if (opcion >= 0 && opcion < autores.size()) {
                var autorSeleccionado = autores.get(opcion);
                mostrarDatosAutor(autorSeleccionado);
            } else {
                System.out.println("\nOpción no válida.");
            }
        } else {
            // Si solo hay un autor, mostrar los datos directamente
            var autor = autores.get(0);
            mostrarDatosAutor(autor);
        }
    }

    // Método para mostrar los detalles del autor
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




    @Transactional
    private void buscarLibroPorAutorYFecha() {
        System.out.println("Ingrese el año para filtrar autores:");
        try {
            int anoSeleccionado = teclado.nextInt();
            teclado.nextLine(); // Limpiar buffer

            var autoresVivos = autorRepository.findAutoresVivosEnAno(anoSeleccionado);

            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos en el año " + anoSeleccionado + ".");
                return;
            }

            System.out.println("Autores vivos en el año " + anoSeleccionado + ":");
            for (int i = 0; i < autoresVivos.size(); i++) {
                var autor = autoresVivos.get(i);
                System.out.printf("%d - %s (Nacimiento: %d, Fallecimiento: %s)\n", i + 1, autor.getNombre(),
                        autor.getFechaDeNacimiento(),
                        autor.getFechaDeFallecimiento() == null ? "N/A" : autor.getFechaDeFallecimiento());
            }

            System.out.println("Seleccione un autor (ingrese el número correspondiente):");
            int opcion = teclado.nextInt();
            teclado.nextLine();

            if (opcion < 1 || opcion > autoresVivos.size()) {
                System.out.println("Opción inválida.");
                return;
            }

            var autorSeleccionado = autoresVivos.get(opcion - 1);
            var libros = autorSeleccionado.getLibros();

            if (libros.isEmpty()) {
                System.out.println("El autor seleccionado no tiene libros registrados.");
            } else {
                System.out.println("Libros del autor " + autorSeleccionado.getNombre() + ":");
                libros.forEach(libro -> System.out.println(" - " + libro.getTitulo()));
            }
        } catch (InputMismatchException e) {
            System.out.println("Entrada no válida. Por favor, ingrese un número para el año.");
            teclado.nextLine(); // Limpiar buffer
        }
    }



}
