/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.authorization.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.repository.CryptographerAuxiliaryRepository;
import eu.arrowhead.authorization.jpa.repository.EncryptionKeyRepository;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.common.exception.InternalServerError;

@ExtendWith(MockitoExtension.class)
public class EncryptionKeyDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private EncryptionKeyDbService dbService;

	@Mock
	private EncryptionKeyRepository keyRepo;

	@Mock
	private CryptographerAuxiliaryRepository auxiliaryRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.get(null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.get(" "));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetInternalServerError() {
		when(keyRepo.findBySystemName("TestProvider")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.get("TestProvider"));

		assertEquals("Database operation error", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetOk() {
		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());

		final Optional<EncryptionKey> result = dbService.get("TestProvider");

		assertTrue(result.isEmpty());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1CandidateNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save((EncryptionKeyModel) null));

		assertEquals("EncryptionKeyModel candidate is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1SystemNameNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1SystemNameEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"",
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1EncryptedValueNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.keyEncryptedValue is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1EncryptedValueEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				" ",
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.keyEncryptedValue is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1AlgorithmNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.algorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1AlgorithmEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"",
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.algorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1InternalAuxiliaryNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.internalAuxiliary is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1InternalAuxiliaryEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"",
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(model));

		assertEquals("EncryptionKeyModel.internalAuxiliary is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1InternalServerError() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		when(keyRepo.findBySystemName("TestProvider")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.save(model));

		assertEquals("Database operation error", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1NoOverrideNoExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());
		when(auxiliaryRepo.saveAndFlush(internal)).thenReturn(internal);
		when(keyRepo.saveAndFlush(key)).thenReturn(savedKey);

		final Pair<EncryptionKey, Boolean> result = dbService.save(model);

		assertEquals(savedKey, result.getFirst());
		assertTrue(result.getSecond());

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo, never()).delete(any(EncryptionKey.class));
		verify(auxiliaryRepo, never()).delete(any(CryptographerAuxiliary.class));
		verify(auxiliaryRepo).saveAndFlush(internal);
		verify(keyRepo).saveAndFlush(key);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave1OverrideNoExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		final CryptographerAuxiliary oldInternal = new CryptographerAuxiliary("oldInternalAux");

		final EncryptionKey oldKey = new EncryptionKey(
				"TestProvider",
				"oldEncryptedValue",
				"alg",
				oldInternal,
				null);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(oldKey));
		doNothing().when(keyRepo).delete(oldKey);
		doNothing().when(auxiliaryRepo).delete(oldInternal);
		when(auxiliaryRepo.saveAndFlush(internal)).thenReturn(internal);
		when(keyRepo.saveAndFlush(key)).thenReturn(savedKey);

		final Pair<EncryptionKey, Boolean> result = dbService.save(model);

		assertEquals(savedKey, result.getFirst());
		assertFalse(result.getSecond());

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).delete(oldKey);
		verify(auxiliaryRepo).delete(oldInternal);
		verify(auxiliaryRepo).saveAndFlush(internal);
		verify(keyRepo).saveAndFlush(key);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSave1OverrideExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				"externalAux");

		final CryptographerAuxiliary oldInternal = new CryptographerAuxiliary("oldInternalAux");
		oldInternal.setId(1);
		final CryptographerAuxiliary oldExternal = new CryptographerAuxiliary("oldExternalAux");
		oldExternal.setId(2);

		final EncryptionKey oldKey = new EncryptionKey(
				"TestProvider",
				"oldEncryptedValue",
				"alg",
				oldInternal,
				oldExternal);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(3);

		final CryptographerAuxiliary external = new CryptographerAuxiliary("externalAux");
		external.setId(4);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				external);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				external);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(oldKey));
		doNothing().when(keyRepo).delete(oldKey);
		doNothing().when(auxiliaryRepo).delete(oldInternal);
		doNothing().when(auxiliaryRepo).delete(oldExternal);
		when(auxiliaryRepo.saveAndFlush(any(CryptographerAuxiliary.class))).thenReturn(internal, external);
		when(keyRepo.saveAndFlush(key)).thenReturn(savedKey);

		final Pair<EncryptionKey, Boolean> result = dbService.save(model);

		assertEquals(savedKey, result.getFirst());
		assertFalse(result.getSecond());

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).delete(oldKey);
		verify(auxiliaryRepo).delete(oldInternal);
		verify(auxiliaryRepo).delete(oldExternal);
		verify(auxiliaryRepo, times(2)).saveAndFlush(any(CryptographerAuxiliary.class));
		verify(keyRepo).saveAndFlush(key);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2InputListNull() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save((List) null));

		assertEquals("EncryptionKeyModel candidate list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2NullCandidate() {
		final List<EncryptionKeyModel> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(list));

		assertEquals("EncryptionKeyModel candidate is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2SystemNameNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel();

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2SystemNameEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"",
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2EncryptedValueNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.keyEncryptedValue is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2EncryptedValueEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"  ",
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.keyEncryptedValue is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2AlgorithmNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.algorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2AlgorithmEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"",
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.algorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2InternalAuxiliaryNull() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.internalAuxiliary is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2InternalAuxiliaryEmpty() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"",
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model)));

		assertEquals("EncryptionKeyModel.internalAuxiliary is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2Duplicate() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(List.of(model, model)));

		assertEquals("Duplicate EncryptionKeyModel for system: TestProvider", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2InternalServerError() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		when(keyRepo.findBySystemName("TestProvider")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.save(List.of(model)));

		assertEquals("Database operation error", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSave2NoOverrideNoExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				null);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());
		doNothing().when(keyRepo).deleteAll(List.of());
		doNothing().when(auxiliaryRepo).deleteAll(List.of());
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();
		when(auxiliaryRepo.saveAllAndFlush(List.of(internal))).thenReturn(List.of(internal));
		when(keyRepo.saveAllAndFlush(List.of(key))).thenReturn(List.of(savedKey));

		final List<EncryptionKey> result = dbService.save(List.of(model));

		assertFalse(result.isEmpty());
		assertEquals(savedKey, result.get(0));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).deleteAll(List.of());
		verify(auxiliaryRepo).deleteAll(List.of());
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
		verify(auxiliaryRepo).saveAllAndFlush(List.of(internal));
		verify(keyRepo).saveAllAndFlush(List.of(key));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSave2OverrideNoExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				"");

		final CryptographerAuxiliary oldInternal = new CryptographerAuxiliary("oldInternalAux");
		oldInternal.setId(1);

		final EncryptionKey oldKey = new EncryptionKey(
				"TestProvider",
				"oldEncryptedValue",
				"alg",
				oldInternal,
				null);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(3);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				null);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(oldKey));
		doNothing().when(keyRepo).deleteAll(List.of(oldKey));
		doNothing().when(auxiliaryRepo).deleteAll(List.of(oldInternal));
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();
		when(auxiliaryRepo.saveAllAndFlush(anyList())).thenReturn(List.of(internal));
		when(keyRepo.saveAllAndFlush(List.of(key))).thenReturn(List.of(savedKey));

		final List<EncryptionKey> result = dbService.save(List.of(model));

		assertFalse(result.isEmpty());
		assertEquals(savedKey, result.get(0));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).deleteAll(List.of(oldKey));
		verify(auxiliaryRepo).deleteAll(List.of(oldInternal));
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
		verify(auxiliaryRepo).saveAllAndFlush(anyList());
		verify(keyRepo).saveAllAndFlush(List.of(key));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testSave2OverrideExternal() {
		final EncryptionKeyModel model = new EncryptionKeyModel(
				"TestProvider",
				"value",
				"encryptedValue",
				"alg",
				"internalAux",
				"externalAux");

		final CryptographerAuxiliary oldInternal = new CryptographerAuxiliary("oldInternalAux");
		oldInternal.setId(1);
		final CryptographerAuxiliary oldExternal = new CryptographerAuxiliary("oldExternalAux");
		oldExternal.setId(2);

		final EncryptionKey oldKey = new EncryptionKey(
				"TestProvider",
				"oldEncryptedValue",
				"alg",
				oldInternal,
				oldExternal);

		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(3);
		final CryptographerAuxiliary external = new CryptographerAuxiliary("externalAux");
		external.setId(4);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				external);

		final EncryptionKey savedKey = new EncryptionKey(
				"TestProvider",
				"encryptedValue",
				"alg",
				internal,
				external);
		savedKey.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(oldKey));
		doNothing().when(keyRepo).deleteAll(List.of(oldKey));
		doNothing().when(auxiliaryRepo).deleteAll(List.of(oldInternal, oldExternal));
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();
		when(auxiliaryRepo.saveAllAndFlush(anyList())).thenReturn(List.of(internal, external));
		when(keyRepo.saveAllAndFlush(List.of(key))).thenReturn(List.of(savedKey));

		final List<EncryptionKey> result = dbService.save(List.of(model));

		assertFalse(result.isEmpty());
		assertEquals(savedKey, result.get(0));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).deleteAll(List.of(oldKey));
		verify(auxiliaryRepo).deleteAll(List.of(oldInternal, oldExternal));
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
		verify(auxiliaryRepo).saveAllAndFlush(anyList());
		verify(keyRepo).saveAllAndFlush(List.of(key));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1SystemNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete((String) null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1SystemNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete(""));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1InternalServerError() {
		when(keyRepo.findBySystemName("TestProvider")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.delete("TestProvider"));

		assertEquals("Database operation error", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1NotFound() {
		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());

		final boolean result = dbService.delete("TestProvider");

		assertFalse(result);

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1NoExternal() {
		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(1);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedKey",
				"alg",
				internal,
				null);
		key.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(key));
		doNothing().when(keyRepo).deleteById(1L);
		doNothing().when(auxiliaryRepo).deleteById(1L);
		doNothing().when(keyRepo).flush();
		doNothing().when(auxiliaryRepo).flush();

		final boolean result = dbService.delete("TestProvider");

		assertTrue(result);

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).deleteById(1L);
		verify(auxiliaryRepo).deleteById(1L);
		verify(keyRepo).flush();
		verify(auxiliaryRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete1External() {
		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(1);

		final CryptographerAuxiliary external = new CryptographerAuxiliary("externalAux");
		external.setId(2);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedKey",
				"alg",
				internal,
				external);
		key.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(key));
		doNothing().when(keyRepo).deleteById(1L);
		doNothing().when(auxiliaryRepo).deleteById(1L);
		doNothing().when(auxiliaryRepo).deleteById(2L);
		doNothing().when(keyRepo).flush();
		doNothing().when(auxiliaryRepo).flush();

		final boolean result = dbService.delete("TestProvider");

		assertTrue(result);

		verify(keyRepo).findBySystemName("TestProvider");
		verify(keyRepo).deleteById(1L);
		verify(auxiliaryRepo).deleteById(1L);
		verify(auxiliaryRepo).deleteById(2L);
		verify(keyRepo).flush();
		verify(auxiliaryRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2ListNull() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete((List) null));

		assertEquals("systemNames list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2ListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete(List.of()));

		assertEquals("systemNames list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2ListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete(list));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2ListContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.delete(List.of("")));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2InternalServerError() {
		when(keyRepo.findBySystemName("TestProvider")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.delete(List.of("TestProvider")));

		assertEquals("Database operation error", ex.getMessage());

		verify(keyRepo).findBySystemName("TestProvider");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2NotFound() {
		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.empty());
		doNothing().when(auxiliaryRepo).deleteAllInBatch(List.of());
		doNothing().when(keyRepo).deleteAllInBatch(List.of());
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();

		assertDoesNotThrow(() -> dbService.delete(List.of("TestProvider")));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(auxiliaryRepo).deleteAllInBatch(List.of());
		verify(keyRepo).deleteAllInBatch(List.of());
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2NoExternal() {
		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(1);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedKey",
				"alg",
				internal,
				null);
		key.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(key));
		doNothing().when(auxiliaryRepo).deleteAllInBatch(List.of(internal));
		doNothing().when(keyRepo).deleteAllInBatch(List.of(key));
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();

		assertDoesNotThrow(() -> dbService.delete(List.of("TestProvider")));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(auxiliaryRepo).deleteAllInBatch(List.of(internal));
		verify(keyRepo).deleteAllInBatch(List.of(key));
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDelete2External() {
		final CryptographerAuxiliary internal = new CryptographerAuxiliary("internalAux");
		internal.setId(1);

		final CryptographerAuxiliary external = new CryptographerAuxiliary("externalAux");
		external.setId(2);

		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"encryptedKey",
				"alg",
				internal,
				external);
		key.setId(1);

		when(keyRepo.findBySystemName("TestProvider")).thenReturn(Optional.of(key));
		doNothing().when(auxiliaryRepo).deleteAllInBatch(List.of(internal, external));
		doNothing().when(keyRepo).deleteAllInBatch(List.of(key));
		doNothing().when(auxiliaryRepo).flush();
		doNothing().when(keyRepo).flush();

		assertDoesNotThrow(() -> dbService.delete(List.of("TestProvider")));

		verify(keyRepo).findBySystemName("TestProvider");
		verify(auxiliaryRepo).deleteAllInBatch(List.of(internal, external));
		verify(keyRepo).deleteAllInBatch(List.of(key));
		verify(auxiliaryRepo).flush();
		verify(keyRepo).flush();
	}
}