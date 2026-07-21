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
            SELECT id, name, ip_address, mac_address, status, created_at, updated_at
            FROM device
            ORDER BY created_at DESC
            """)
    List<DeviceResponse> findAll();

    @Select("SELECT COUNT(*) FROM device")
    long count();

    @Select("""
            SELECT id, name, ip_address, mac_address, status, created_at, updated_at
            FROM device
            WHERE id = #{id}
            """)
    DeviceResponse findById(@Param("id") Long id);

    @Select("""
            SELECT EXISTS(
                SELECT 1
                FROM device
                WHERE mac_address = #{macAddress}
            )
            """)
    boolean existsByMacAddress(@Param("macAddress") String macAddress);

    @Select("""
            SELECT EXISTS(
                SELECT 1
                FROM device
                WHERE mac_address = #{macAddress}
                  AND id <> #{id}
            )
            """)
    boolean existsByMacAddressAndIdNot(@Param("macAddress") String macAddress, @Param("id") Long id);

    @Insert("""
            INSERT INTO device (name, ip_address, mac_address, status)
            VALUES (#{name}, #{ipAddress}, #{macAddress}, #{status})
            """)
    int insert(DeviceResponse device);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("""
            UPDATE device
            SET name = #{name},
                ip_address = #{ipAddress},
                mac_address = #{macAddress},
                status = #{status}
            WHERE id = #{id}
            """)
    int update(DeviceResponse device);
}
