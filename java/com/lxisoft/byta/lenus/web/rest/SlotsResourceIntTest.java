package com.lxisoft.byta.lenus.web.rest;

import com.lxisoft.byta.lenus.DoctorApp;

import com.lxisoft.byta.lenus.domain.Slots;
import com.lxisoft.byta.lenus.repository.SlotsRepository;
import com.lxisoft.byta.lenus.service.SlotsService;
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

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;

import static com.lxisoft.byta.lenus.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the SlotsResource REST controller.
 *
 * @see SlotsResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DoctorApp.class)
public class SlotsResourceIntTest {

    private static final ZonedDateTime DEFAULT_START_TIME = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_START_TIME = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_END_TIME = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_END_TIME = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    @Autowired
    private SlotsRepository slotsRepository;

    @Autowired
    private SlotsService slotsService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restSlotsMockMvc;

    private Slots slots;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SlotsResource slotsResource = new SlotsResource(slotsService);
        this.restSlotsMockMvc = MockMvcBuilders.standaloneSetup(slotsResource)
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
    public static Slots createEntity(EntityManager em) {
        Slots slots = new Slots()
            .startTime(DEFAULT_START_TIME)
            .endTime(DEFAULT_END_TIME)
            .active(DEFAULT_ACTIVE);
        return slots;
    }

    @Before
    public void initTest() {
        slots = createEntity(em);
    }

    @Test
    @Transactional
    public void createSlots() throws Exception {
        int databaseSizeBeforeCreate = slotsRepository.findAll().size();

        // Create the Slots
        restSlotsMockMvc.perform(post("/api/slots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(slots)))
            .andExpect(status().isCreated());

        // Validate the Slots in the database
        List<Slots> slotsList = slotsRepository.findAll();
        assertThat(slotsList).hasSize(databaseSizeBeforeCreate + 1);
        Slots testSlots = slotsList.get(slotsList.size() - 1);
        assertThat(testSlots.getStartTime()).isEqualTo(DEFAULT_START_TIME);
        assertThat(testSlots.getEndTime()).isEqualTo(DEFAULT_END_TIME);
        assertThat(testSlots.isActive()).isEqualTo(DEFAULT_ACTIVE);
    }

    @Test
    @Transactional
    public void createSlotsWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = slotsRepository.findAll().size();

        // Create the Slots with an existing ID
        slots.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSlotsMockMvc.perform(post("/api/slots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(slots)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Slots> slotsList = slotsRepository.findAll();
        assertThat(slotsList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllSlots() throws Exception {
        // Initialize the database
        slotsRepository.saveAndFlush(slots);

        // Get all the slotsList
        restSlotsMockMvc.perform(get("/api/slots?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(slots.getId().intValue())))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(sameInstant(DEFAULT_START_TIME))))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(sameInstant(DEFAULT_END_TIME))))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE.booleanValue())));
    }

    @Test
    @Transactional
    public void getSlots() throws Exception {
        // Initialize the database
        slotsRepository.saveAndFlush(slots);

        // Get the slots
        restSlotsMockMvc.perform(get("/api/slots/{id}", slots.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(slots.getId().intValue()))
            .andExpect(jsonPath("$.startTime").value(sameInstant(DEFAULT_START_TIME)))
            .andExpect(jsonPath("$.endTime").value(sameInstant(DEFAULT_END_TIME)))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingSlots() throws Exception {
        // Get the slots
        restSlotsMockMvc.perform(get("/api/slots/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSlots() throws Exception {
        // Initialize the database
        slotsService.save(slots);

        int databaseSizeBeforeUpdate = slotsRepository.findAll().size();

        // Update the slots
        Slots updatedSlots = slotsRepository.findOne(slots.getId());
        updatedSlots
            .startTime(UPDATED_START_TIME)
            .endTime(UPDATED_END_TIME)
            .active(UPDATED_ACTIVE);

        restSlotsMockMvc.perform(put("/api/slots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedSlots)))
            .andExpect(status().isOk());

        // Validate the Slots in the database
        List<Slots> slotsList = slotsRepository.findAll();
        assertThat(slotsList).hasSize(databaseSizeBeforeUpdate);
        Slots testSlots = slotsList.get(slotsList.size() - 1);
        assertThat(testSlots.getStartTime()).isEqualTo(UPDATED_START_TIME);
        assertThat(testSlots.getEndTime()).isEqualTo(UPDATED_END_TIME);
        assertThat(testSlots.isActive()).isEqualTo(UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    public void updateNonExistingSlots() throws Exception {
        int databaseSizeBeforeUpdate = slotsRepository.findAll().size();

        // Create the Slots

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restSlotsMockMvc.perform(put("/api/slots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(slots)))
            .andExpect(status().isCreated());

        // Validate the Slots in the database
        List<Slots> slotsList = slotsRepository.findAll();
        assertThat(slotsList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteSlots() throws Exception {
        // Initialize the database
        slotsService.save(slots);

        int databaseSizeBeforeDelete = slotsRepository.findAll().size();

        // Get the slots
        restSlotsMockMvc.perform(delete("/api/slots/{id}", slots.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Slots> slotsList = slotsRepository.findAll();
        assertThat(slotsList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Slots.class);
        Slots slots1 = new Slots();
        slots1.setId(1L);
        Slots slots2 = new Slots();
        slots2.setId(slots1.getId());
        assertThat(slots1).isEqualTo(slots2);
        slots2.setId(2L);
        assertThat(slots1).isNotEqualTo(slots2);
        slots1.setId(null);
        assertThat(slots1).isNotEqualTo(slots2);
    }
}
