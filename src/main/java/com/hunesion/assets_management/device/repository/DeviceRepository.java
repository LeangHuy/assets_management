package com.hunesion.assets_management.device.repository;

import com.hunesion.assets_management.device.dto.DeviceResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DeviceRepository {

    @Select("""
            SELECT id, name, ip_address, status, created_at, updated_at
            FROM device
            ORDER BY created_at DESC
            """)
    List<DeviceResponse> findAll();

    @Select("SELECT COUNT(*) FROM device WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM device")
    long countAllDevice();

    @Select("""
            SELECT id, name, ip_address, status, created_at, updated_at
            FROM device
            WHERE id = #{id}
            """)
    DeviceResponse findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO device (name, ip_address, status)
            VALUES (#{name}, #{ipAddress}, #{status})
            """)
    int insert(DeviceResponse device);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("""
            UPDATE device
            SET name = #{name},
                ip_address = #{ipAddress},
                status = #{status}
            WHERE id = #{id}
            """)
    int update(DeviceResponse device);

    @Update("""
            UPDATE device
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
