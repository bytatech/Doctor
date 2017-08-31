package com.lxisoft.byta.lenus.web.rest;

import com.lxisoft.byta.lenus.DoctorApp;

import com.lxisoft.byta.lenus.domain.Doctor;
import com.lxisoft.byta.lenus.repository.DoctorRepository;
import com.lxisoft.byta.lenus.service.DoctorService;
import com.lxisoft.byta.lenus.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the DoctorResource REST controller.
 *
 * @see DoctorResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DoctorApp.class)
public class DoctorResourceIntTest {

    private static final String DEFAULT_DOCTOR_NAME = "AAAAAAAAAA";
    private static final String UPDATED_DOCTOR_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_QUALIFICATION = "AAAAAAAAAA";
    private static final String UPDATED_QUALIFICATION = "BBBBBBBBBB";

    private static final String DEFAULT_SPECIALIZATION = "AAAAAAAAAA";
    private static final String UPDATED_SPECIALIZATION = "BBBBBBBBBB";

    private static final String DEFAULT_LICENSE_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_LICENSE_NUMBER = "BBBBBBBBBB";

    private static final byte[] DEFAULT_DOCTOR_IMAGE = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_DOCTOR_IMAGE = TestUtil.createByteArray(2, "1");
    private static final String DEFAULT_DOCTOR_IMAGE_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_DOCTOR_IMAGE_CONTENT_TYPE = "image/png";

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restDoctorMockMvc;

    private Doctor doctor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DoctorResource doctorResource = new DoctorResource(doctorService);
        this.restDoctorMockMvc = MockMvcBuilders.standaloneSetup(doctorResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Doctor createEntity(EntityManager em) {
        Doctor doctor = new Doctor()
            .doctorName(DEFAULT_DOCTOR_NAME)
            .qualification(DEFAULT_QUALIFICATION)
            .specialization(DEFAULT_SPECIALIZATION)
            .licenseNumber(DEFAULT_LICENSE_NUMBER)
            .doctorImage(DEFAULT_DOCTOR_IMAGE)
            .doctorImageContentType(DEFAULT_DOCTOR_IMAGE_CONTENT_TYPE);
        return doctor;
    }

    @Before
    public void initTest() {
        doctor = createEntity(em);
    }

    @Test
    @Transactional
    public void createDoctor() throws Exception {
        int databaseSizeBeforeCreate = doctorRepository.findAll().size();

        // Create the Doctor
        restDoctorMockMvc.perform(post("/api/doctors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(doctor)))
            .andExpect(status().isCreated());

        // Validate the Doctor in the database
        List<Doctor> doctorList = doctorRepository.findAll();
        assertThat(doctorList).hasSize(databaseSizeBeforeCreate + 1);
        Doctor testDoctor = doctorList.get(doctorList.size() - 1);
        assertThat(testDoctor.getDoctorName()).isEqualTo(DEFAULT_DOCTOR_NAME);
        assertThat(testDoctor.getQualification()).isEqualTo(DEFAULT_QUALIFICATION);
        assertThat(testDoctor.getSpecialization()).isEqualTo(DEFAULT_SPECIALIZATION);
        assertThat(testDoctor.getLicenseNumber()).isEqualTo(DEFAULT_LICENSE_NUMBER);
        assertThat(testDoctor.getDoctorImage()).isEqualTo(DEFAULT_DOCTOR_IMAGE);
        assertThat(testDoctor.getDoctorImageContentType()).isEqualTo(DEFAULT_DOCTOR_IMAGE_CONTENT_TYPE);
    }

    @Test
    @Transactional
    public void createDoctorWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = doctorRepository.findAll().size();

        // Create the Doctor with an existing ID
        doctor.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restDoctorMockMvc.perform(post("/api/doctors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(doctor)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Doctor> doctorList = doctorRepository.findAll();
        assertThat(doctorList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllDoctors() throws Exception {
        // Initialize the database
        doctorRepository.saveAndFlush(doctor);

        // Get all the doctorList
        restDoctorMockMvc.perform(get("/api/doctors?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(doctor.getId().intValue())))
            .andExpect(jsonPath("$.[*].doctorName").value(hasItem(DEFAULT_DOCTOR_NAME.toString())))
            .andExpect(jsonPath("$.[*].qualification").value(hasItem(DEFAULT_QUALIFICATION.toString())))
            .andExpect(jsonPath("$.[*].specialization").value(hasItem(DEFAULT_SPECIALIZATION.toString())))
            .andExpect(jsonPath("$.[*].licenseNumber").value(hasItem(DEFAULT_LICENSE_NUMBER.toString())))
            .andExpect(jsonPath("$.[*].doctorImageContentType").value(hasItem(DEFAULT_DOCTOR_IMAGE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].doctorImage").value(hasItem(Base64Utils.encodeToString(DEFAULT_DOCTOR_IMAGE))));
    }

    @Test
    @Transactional
    public void getDoctor() throws Exception {
        // Initialize the database
        doctorRepository.saveAndFlush(doctor);

        // Get the doctor
        restDoctorMockMvc.perform(get("/api/doctors/{id}", doctor.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(doctor.getId().intValue()))
            .andExpect(jsonPath("$.doctorName").value(DEFAULT_DOCTOR_NAME.toString()))
            .andExpect(jsonPath("$.qualification").value(DEFAULT_QUALIFICATION.toString()))
            .andExpect(jsonPath("$.specialization").value(DEFAULT_SPECIALIZATION.toString()))
            .andExpect(jsonPath("$.licenseNumber").value(DEFAULT_LICENSE_NUMBER.toString()))
            .andExpect(jsonPath("$.doctorImageContentType").value(DEFAULT_DOCTOR_IMAGE_CONTENT_TYPE))
            .andExpect(jsonPath("$.doctorImage").value(Base64Utils.encodeToString(DEFAULT_DOCTOR_IMAGE)));
    }

    @Test
    @Transactional
    public void getNonExistingDoctor() throws Exception {
        // Get the doctor
        restDoctorMockMvc.perform(get("/api/doctors/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateDoctor() throws Exception {
        // Initialize the database
        doctorService.save(doctor);

        int databaseSizeBeforeUpdate = doctorRepository.findAll().size();

        // Update the doctor
        Doctor updatedDoctor = doctorRepository.findOne(doctor.getId());
        updatedDoctor
            .doctorName(UPDATED_DOCTOR_NAME)
            .qualification(UPDATED_QUALIFICATION)
            .specialization(UPDATED_SPECIALIZATION)
            .licenseNumber(UPDATED_LICENSE_NUMBER)
            .doctorImage(UPDATED_DOCTOR_IMAGE)
            .doctorImageContentType(UPDATED_DOCTOR_IMAGE_CONTENT_TYPE);

        restDoctorMockMvc.perform(put("/api/doctors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedDoctor)))
            .andExpect(status().isOk());

        // Validate the Doctor in the database
        List<Doctor> doctorList = doctorRepository.findAll();
        assertThat(doctorList).hasSize(databaseSizeBeforeUpdate);
        Doctor testDoctor = doctorList.get(doctorList.size() - 1);
        assertThat(testDoctor.getDoctorName()).isEqualTo(UPDATED_DOCTOR_NAME);
        assertThat(testDoctor.getQualification()).isEqualTo(UPDATED_QUALIFICATION);
        assertThat(testDoctor.getSpecialization()).isEqualTo(UPDATED_SPECIALIZATION);
        assertThat(testDoctor.getLicenseNumber()).isEqualTo(UPDATED_LICENSE_NUMBER);
        assertThat(testDoctor.getDoctorImage()).isEqualTo(UPDATED_DOCTOR_IMAGE);
        assertThat(testDoctor.getDoctorImageContentType()).isEqualTo(UPDATED_DOCTOR_IMAGE_CONTENT_TYPE);
    }

    @Test
    @Transactional
    public void updateNonExistingDoctor() throws Exception {
        int databaseSizeBeforeUpdate = doctorRepository.findAll().size();

        // Create the Doctor

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restDoctorMockMvc.perform(put("/api/doctors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(doctor)))
            .andExpect(status().isCreated());

        // Validate the Doctor in the database
        List<Doctor> doctorList = doctorRepository.findAll();
        assertThat(doctorList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteDoctor() throws Exception {
        // Initialize the database
        doctorService.save(doctor);

        int databaseSizeBeforeDelete = doctorRepository.findAll().size();

        // Get the doctor
        restDoctorMockMvc.perform(delete("/api/doctors/{id}", doctor.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Doctor> doctorList = doctorRepository.findAll();
        assertThat(doctorList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Doctor.class);
        Doctor doctor1 = new Doctor();
        doctor1.setId(1L);
        Doctor doctor2 = new Doctor();
        doctor2.setId(doctor1.getId());
        assertThat(doctor1).isEqualTo(doctor2);
        doctor2.setId(2L);
        assertThat(doctor1).isNotEqualTo(doctor2);
        doctor1.setId(null);
        assertThat(doctor1).isNotEqualTo(doctor2);
    }
}
