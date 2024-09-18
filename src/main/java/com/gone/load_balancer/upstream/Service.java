package com.gone.load_balancer.upstream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private String serviceName;
    private String ip;
    private int weight = 1;
    private String status;
}
