package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OtpAttemptService {

    @Autowired
    private OtpRepository otpRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFailedAttempt(String email) {
        otpRepository.incrementFailedAttempts(email);
    }
}