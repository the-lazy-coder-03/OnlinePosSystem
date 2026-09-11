package org.example.onlinepossystem.staff.service;

import org.example.onlinepossystem.staff.entity.Staff;
import org.example.onlinepossystem.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffServiceDependencyTest {
    @Test
    void createStaffUsesInjectedPasswordEncoder() {
        StaffRepository repository = mock(StaffRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("1234")).thenReturn("encoded-pin");
        when(repository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StaffService service = new StaffService(repository, encoder);

        service.createStaff("Sam", "Kenridge", "1234", "BRANCH-CODE");

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(encoder).encode("1234");
        verify(repository).save(staffCaptor.capture());
        assertThat(staffCaptor.getValue().getPinHash()).isEqualTo("encoded-pin");
    }
}
