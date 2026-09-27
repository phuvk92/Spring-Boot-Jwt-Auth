package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateVehicleConfigurationRequest;
import com.example.svgmanager.dto.request.UpdateVehicleConfigurationRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.CategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleConfigurationRepository;
import com.example.svgmanager.service.impl.VehicleConfigurationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleConfigurationServiceTest {

    @Mock
    private VehicleConfigurationRepository configurationRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CarBrandService carBrandService;

    @Mock
    private CarModelService carModelService;

    @Mock
    private SvgFileRepository svgFileRepository;

    @Mock
    private AuditLogService auditLogService;

    private VehicleConfigurationMapper mapper;
    private VehicleConfigurationServiceImpl service;

    private Category testCategory;
    private CarBrand testBrandToyota;
    private CarBrand testBrandHonda;
    private CarModel testModelCamry;
    private CarModel testModelCivic;
    private VehicleConfiguration testConfig;

    @BeforeEach
    void setUp() {
        mapper = new VehicleConfigurationMapper();
        service = new VehicleConfigurationServiceImpl(
                configurationRepository,
                categoryRepository,
                carBrandService,
                carModelService,
                svgFileRepository,
                mapper,
                auditLogService
        );

        testCategory = new Category(1L, "Ngoại thất", "PPF Exterior", "category", null, null, 1, LocalDateTime.now());

        testBrandToyota = CarBrand.builder()
                .id(1L)
                .code("TOYOTA")
                .name("Toyota")
                .status("ACTIVE")
                .displayOrder(1)
                .build();

        testBrandHonda = CarBrand.builder()
                .id(2L)
                .code("HONDA")
                .name("Honda")
                .status("ACTIVE")
                .displayOrder(2)
                .build();

        testModelCamry = CarModel.builder()
                .id(10L)
                .brand(testBrandToyota)
                .code("CAMRY")
                .name("Camry")
                .status("ACTIVE")
                .build();

        testModelCivic = CarModel.builder()
                .id(20L)
                .brand(testBrandHonda)
                .code("CIVIC")
                .name("Civic")
                .status("ACTIVE")
                .build();

        testConfig = VehicleConfiguration.builder()
                .id(100L)
                .category(testCategory)
                .productGroup(ProductGroup.PPF_EXTERIOR)
                .brand(testBrandToyota)
                .model(testModelCamry)
                .yearFrom(2019)
                .yearTo(2023)
                .generationCode("XV70")
                .status("ACTIVE")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- CREATE TESTS ---

    @Test
    @DisplayName("Create valid vehicle configuration successfully")
    void createConfiguration_Valid_Success() {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2019, 2023, "XV70", "ACTIVE"
        );

        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(10L)).thenReturn(testModelCamry);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(configurationRepository.existsByCategoryIdAndBrandIdAndModelIdAndYearFromAndYearToAndGenerationCodeIgnoreCaseAndDeletedFalse(
                eq(1L), eq(1L), eq(10L), eq(2019), eq(2023), eq("XV70")
        )).thenReturn(false);
        when(configurationRepository.save(any(VehicleConfiguration.class))).thenAnswer(invocation -> {
            VehicleConfiguration vc = invocation.getArgument(0);
            vc.setId(101L);
            return vc;
        });

        VehicleConfigurationResponse response = service.createConfiguration(request, "admin", "ADMIN");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.generationCode()).isEqualTo("XV70");
        assertThat(response.brand().name()).isEqualTo("Toyota");
        assertThat(response.model().name()).isEqualTo("Camry");
        assertThat(response.yearFrom()).isEqualTo(2019);
        assertThat(response.yearTo()).isEqualTo(2023);

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("CREATE_VEHICLE_CONFIGURATION"), eq("VehicleConfiguration"), eq(101L), anyString());
    }

    @Test
    @DisplayName("Create rejected when yearFrom > yearTo")
    void createConfiguration_InvalidYearRange_ThrowsBadRequest() {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2025, 2020, "XV70", "ACTIVE"
        );

        assertThatThrownBy(() -> service.createConfiguration(request, "admin", "ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không được lớn hơn năm đến");

        verifyNoInteractions(configurationRepository);
    }

    @Test
    @DisplayName("Create rejected when model does not belong to selected brand")
    void createConfiguration_ModelNotBelongToBrand_ThrowsBadRequest() {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 20L, 2020, 2024, "FC", "ACTIVE"
        );

        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(20L)).thenReturn(testModelCivic); // Civic belongs to Honda (id: 2)

        assertThatThrownBy(() -> service.createConfiguration(request, "admin", "ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không thuộc Hãng xe");

        verify(configurationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rejected when duplicate vehicle configuration already exists")
    void createConfiguration_Duplicate_ThrowsConflict() {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2019, 2023, "XV70", "ACTIVE"
        );

        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(10L)).thenReturn(testModelCamry);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(configurationRepository.existsByCategoryIdAndBrandIdAndModelIdAndYearFromAndYearToAndGenerationCodeIgnoreCaseAndDeletedFalse(
                eq(1L), eq(1L), eq(10L), eq(2019), eq(2023), eq("XV70")
        )).thenReturn(true);

        assertThatThrownBy(() -> service.createConfiguration(request, "admin", "ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("VEHICLE_CONFIGURATION_ALREADY_EXISTS");

        verify(configurationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rejected when Category not found")
    void createConfiguration_CategoryNotFound_ThrowsNotFound() {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                999L, "PPF_EXTERIOR", 1L, 10L, 2019, 2023, "XV70", "ACTIVE"
        );

        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(10L)).thenReturn(testModelCamry);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createConfiguration(request, "admin", "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy Danh mục");
    }

    // --- UPDATE TESTS ---

    @Test
    @DisplayName("Update valid vehicle configuration successfully")
    void updateConfiguration_Valid_Success() {
        UpdateVehicleConfigurationRequest request = new UpdateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2020, 2024, "XV70-FACELIFT", "ACTIVE"
        );

        when(configurationRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testConfig));
        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(10L)).thenReturn(testModelCamry);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(configurationRepository.existsDuplicateExcludingId(
                eq(1L), eq(1L), eq(10L), eq(2020), eq(2024), eq("XV70-FACELIFT"), eq(100L)
        )).thenReturn(false);
        when(configurationRepository.save(any(VehicleConfiguration.class))).thenReturn(testConfig);

        VehicleConfigurationResponse response = service.updateConfiguration(100L, request, "admin", "ADMIN");

        assertThat(response).isNotNull();
        assertThat(testConfig.getGenerationCode()).isEqualTo("XV70-FACELIFT");
        assertThat(testConfig.getYearFrom()).isEqualTo(2020);
        assertThat(testConfig.getYearTo()).isEqualTo(2024);

        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("UPDATE_VEHICLE_CONFIGURATION"), eq("VehicleConfiguration"), eq(100L), anyString());
    }

    @Test
    @DisplayName("Update rejected when config ID not found")
    void updateConfiguration_NotFound_ThrowsNotFound() {
        UpdateVehicleConfigurationRequest request = new UpdateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2019, 2023, "XV70", "ACTIVE"
        );

        when(configurationRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateConfiguration(999L, request, "admin", "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy Cấu hình xe");
    }

    @Test
    @DisplayName("Update rejected when duplicate conflict exists")
    void updateConfiguration_Duplicate_ThrowsConflict() {
        UpdateVehicleConfigurationRequest request = new UpdateVehicleConfigurationRequest(
                1L, "PPF_EXTERIOR", 1L, 10L, 2019, 2023, "XV70", "ACTIVE"
        );

        when(configurationRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testConfig));
        when(carBrandService.getEntityById(1L)).thenReturn(testBrandToyota);
        when(carModelService.getEntityById(10L)).thenReturn(testModelCamry);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(configurationRepository.existsDuplicateExcludingId(
                eq(1L), eq(1L), eq(10L), eq(2019), eq(2023), eq("XV70"), eq(100L)
        )).thenReturn(true);

        assertThatThrownBy(() -> service.updateConfiguration(100L, request, "admin", "ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("VEHICLE_CONFIGURATION_ALREADY_EXISTS");
    }

    // --- DELETE TESTS ---

    @Test
    @DisplayName("Delete vehicle configuration successfully when not in use")
    void deleteConfiguration_Success() {
        when(configurationRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testConfig));
        when(svgFileRepository.existsByVehicleConfigurationId(100L)).thenReturn(false);

        service.deleteConfiguration(100L, "admin", "ADMIN");

        assertThat(testConfig.isDeleted()).isTrue();
        assertThat(testConfig.getStatus()).isEqualTo("INACTIVE");
        verify(configurationRepository).save(testConfig);
        verify(auditLogService).log(eq("admin"), eq("ADMIN"), eq("DELETE_VEHICLE_CONFIGURATION"), eq("VehicleConfiguration"), eq(100L), anyString());
    }

    @Test
    @DisplayName("Delete rejected when configuration is in use by SVG files")
    void deleteConfiguration_InUse_ThrowsConflict() {
        when(configurationRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(testConfig));
        when(svgFileRepository.existsByVehicleConfigurationId(100L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteConfiguration(100L, "admin", "ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("VEHICLE_CONFIGURATION_IN_USE");

        assertThat(testConfig.isDeleted()).isFalse();
        verify(configurationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete rejected when configuration not found")
    void deleteConfiguration_NotFound_ThrowsNotFound() {
        when(configurationRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteConfiguration(999L, "admin", "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- LIST & FILTER TESTS ---

    @Test
    @DisplayName("Get configurations list with pagination and filters")
    void getConfigurations_Success() {
        Page<VehicleConfiguration> page = new PageImpl<>(List.of(testConfig));
        when(configurationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<VehicleConfigurationResponse> result = service.getConfigurations(
                1L, "PPF_EXTERIOR", 1L, 10L, 2021, "ACTIVE", "XV70", 0, 10, "createdAt", "desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).generationCode()).isEqualTo("XV70");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
