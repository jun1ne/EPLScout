package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import service.ScoutBatchService;

@RestController
public class BatchController {

    @GetMapping("/api/batch/run")
    public String runBatch() {
        try {
            ScoutBatchService batchService = new ScoutBatchService();
            batchService.runFullBatch();
            return "BATCH RUN COMPLETE";
        } catch (Exception e) {
            e.printStackTrace();
            return "BATCH RUN FAILED";
        }
    }
}
