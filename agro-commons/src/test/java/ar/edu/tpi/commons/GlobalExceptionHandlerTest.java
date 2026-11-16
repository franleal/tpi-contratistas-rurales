package ar.edu.tpi.commons;

import ar.edu.tpi.commons.error.GlobalExceptionHandler;
import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record Pedido(@NotBlank String nombre) {
    }

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/regla")
        void regla() {
            throw new ReglaNegocioException("Lote en zona de EXCLUSION (RN-06)");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoEstadoException("Estado incorrecto (RN-18)");
        }

        @GetMapping("/caida")
        void caida() {
            throw new DependenciaNoDisponibleException("Open-Meteo no responde");
        }

        @GetMapping("/bug")
        void bug() {
            throw new IllegalStateException("detalle interno");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Pedido pedido) {
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void reglaDeNegocioDevuelve422ConFormatoComun() throws Exception {
        mvc.perform(get("/regla"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("Lote en zona de EXCLUSION (RN-06)"))
                .andExpect(jsonPath("$.path").value("/regla"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conflictoDevuelve409() throws Exception {
        mvc.perform(get("/conflicto")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test
    void dependenciaCaidaDevuelve503() throws Exception {
        mvc.perform(get("/caida")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));
    }

    @Test
    void errorInesperadoNoFiltraDetalleInterno() throws Exception {
        mvc.perform(get("/bug")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }

    @Test
    void beanValidationDevuelve400ConDetallePorCampo() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("nombre:")));
    }
}
