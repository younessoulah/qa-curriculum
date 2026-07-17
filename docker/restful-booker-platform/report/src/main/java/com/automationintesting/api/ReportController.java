package com.automationintesting.api;

import com.automationintesting.model.report.Report;
import com.automationintesting.requests.AuthRequests;
import com.automationintesting.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    private final AuthRequests authRequest = new AuthRequests();

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Report> getAllRoomReports(@CookieValue(value = "token", required = false) String token) {
        if (!authRequest.postCheckAuth(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Report report = reportService.getAllRoomsReport(token);

        return ResponseEntity.status(HttpStatus.OK).body(report);
    }

    @RequestMapping(value = "/room/{id:[0-9]*}", method = RequestMethod.GET)
    public ResponseEntity<Report> getSpecificRoomReport(@PathVariable(value = "id") int roomId, @CookieValue(value = "token", required = false) String token){
        if (!authRequest.postCheckAuth(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Report report = reportService.getSpecificRoomReport(roomId);

        return ResponseEntity.status(HttpStatus.OK).body(report);
    }

}
