package org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.FilterConditionDTO;
import org.hibernate.annotations.ForeignKey;

/**
 * Dieses Daten-Transfer-Objekt stellt hält die Konfiguration einer
 * AMS_FilterCondition_String.
 * 
 * Das Create-Statement für die Datenbank hat folgendes Aussehen:
 * 
 * <pre>
 *  create table AMS_FilterCondition_String
 *  (
 *  iFilterConditionRef	INT NOT NULL,
 *  cKeyValue		VARCHAR(16),
 *  sOperator		SMALLINT,
 *  cCompValue		VARCHAR(128)
 *  );
 * </pre>
 */
@Entity
@Table(name = "AMS_FilterCondition_String")
@PrimaryKeyJoinColumn(name="iFilterConditionRef", referencedColumnName="iFilterConditionID")
public class StringFilterConditionDTO extends FilterConditionDTO {

	//ForeignKey(name="iFilterConditionID", inverseName="iFilterConditionRef")
	@Column(name="iFilterConditionRef", nullable=false, updatable=false, insertable=false)
	private int filterConditionRef;

	@Column(name = "cKeyValue", length = 16)
	private int keyValue;

	@Column(name = "sOperator")
	private short operator;

	@Column(name = "cCompValue", length = 128)
	private int compValue;

	/**
	 * @return the filterConditionRef
	 */
	@SuppressWarnings("unused")
	private int getFilterConditionRef() {
		return filterConditionRef;
	}

	/**
	 * @param filterConditionRef
	 *            the filterConditionRef to set
	 */
	@SuppressWarnings("unused")
	private void setFilterConditionRef(int filterConditionRef) {
		this.filterConditionRef = filterConditionRef;
	}

	/**
	 * @return the keyValue
	 */
	@SuppressWarnings("unused")
	private int getKeyValue() {
		return keyValue;
	}

	/**
	 * @param keyValue
	 *            the keyValue to set
	 */
	@SuppressWarnings("unused")
	private void setKeyValue(int keyValue) {
		this.keyValue = keyValue;
	}

	/**
	 * @return the operator
	 */
	@SuppressWarnings("unused")
	private short getOperator() {
		return operator;
	}

	/**
	 * @param operator
	 *            the operator to set
	 */
	@SuppressWarnings("unused")
	private void setOperator(short operator) {
		this.operator = operator;
	}

	/**
	 * @return the compValue
	 */
	@SuppressWarnings("unused")
	private int getCompValue() {
		return compValue;
	}

	/**
	 * @param compValue
	 *            the compValue to set
	 */
	@SuppressWarnings("unused")
	private void setCompValue(int compValue) {
		this.compValue = compValue;
	}

}
