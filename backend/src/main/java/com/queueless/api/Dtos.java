package com.queueless.api;

import com.queueless.domain.TokenStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;

public class Dtos {
  public record QueueRequest(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=20) String prefix,@Positive int averageServiceDurationMinutes,@NotNull Boolean open){}
  public record QueueStatusRequest(@NotNull Boolean open){}
  public record JoinRequest(@NotBlank @Size(max=100) String customerName,@Size(max=40) String phone){}
  public record QueueResponse(Long queueId,String name,String prefix,int averageServiceDurationMinutes,boolean open,LocalDate dailySequenceResetOn,int lastSequenceNumber){}
  public record QueueSummary(Long queueId,String name,String prefix,boolean open){}
  public record TokenResponse(Long tokenId,String displayIdentifier,Long queueId,TokenStatus status,Integer position,Integer estimatedWaitMinutes){}
  public record DashboardResponse(Long queueId,Map<TokenStatus,Long> counts,List<ActiveToken> activeTokens){}
  public record ActiveToken(Long tokenId,String displayIdentifier,String customerName,TokenStatus status,Integer sequenceNumber){}
  public record ErrorResponse(String error,String message,List<FieldError> fieldErrors){
    public static ErrorResponse of(String error,String message){return new ErrorResponse(error,message,List.of());}
  }
  public record FieldError(String field,String message){}
}
