package com.mc10inc.biostamp3.sdk.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProvisionedSensorDao {
    @Query("SELECT * FROM provisionedsensor")
    List<ProvisionedSensor> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ProvisionedSensor provisionedSensor);

    @Delete
    void delete(ProvisionedSensor provisionedSensor);
}
