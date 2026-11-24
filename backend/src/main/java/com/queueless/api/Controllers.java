package com.queueless.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queues")
class QueueController {
  private final QueueService queues;
  private final TokenService tokens;
  QueueController(QueueService queues, TokenService tokens){this.queues=queues;this.tokens=tokens;}
  @GetMapping public List<Dtos.QueueSummary> list(){return queues.list();}
  @PostMapping public ResponseEntity<Dtos.QueueResponse> create(@Valid @RequestBody Dtos.QueueRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(queues.create(r));}
  @GetMapping("/{id}") public Dtos.QueueResponse get(@PathVariable Long id){return queues.get(id);}
  @PutMapping("/{id}") public Dtos.QueueResponse update(@PathVariable Long id,@Valid @RequestBody Dtos.QueueRequest r){return queues.update(id,r);}
  @PatchMapping("/{id}/status") public Dtos.QueueResponse status(@PathVariable Long id,@Valid @RequestBody Dtos.QueueStatusRequest r){return queues.setStatus(id,r.open());}
  @PostMapping("/{id}/join") public ResponseEntity<Dtos.TokenResponse> join(@PathVariable Long id,@Valid @RequestBody Dtos.JoinRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(tokens.join(id,r));}
  @PostMapping("/{id}/call-next") public Dtos.TokenResponse callNext(@PathVariable Long id){return tokens.callNext(id);}
  @GetMapping("/{id}/dashboard") public Dtos.DashboardResponse dashboard(@PathVariable Long id){return tokens.dashboard(id);}
}

@RestController
@RequestMapping("/api/tokens")
class TokenController {
  private final TokenService tokens;
  TokenController(TokenService tokens){this.tokens=tokens;}
  @GetMapping("/{id}") public Dtos.TokenResponse snapshot(@PathVariable Long id){return tokens.snapshot(id);}
  @PostMapping("/{id}/cancel") public Dtos.TokenResponse cancel(@PathVariable Long id){return tokens.cancel(id);}
  @PostMapping("/{id}/complete") public Dtos.TokenResponse complete(@PathVariable Long id){return tokens.complete(id);}
  @PostMapping("/{id}/skip") public Dtos.TokenResponse skip(@PathVariable Long id){return tokens.skip(id);}
}
