package com.taskmind.backend.specbreakdown.attachment;
import com.taskmind.backend.auth.AuthenticatedUser; import java.time.Instant; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/v1/spec-breakdown/drafts/{draftId}/attachments")
public class SpecBreakdownAttachmentController { private final Map<UUID,List<Attachment>> attachments=new java.util.concurrent.ConcurrentHashMap<>();
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<Attachment> upload(AuthenticatedUser u,@PathVariable UUID draftId,@RequestPart("file") MultipartFile file){ Attachment a=new Attachment(UUID.randomUUID(),draftId,file.getOriginalFilename(),file.getContentType(),"spec-breakdown/"+draftId+"/"+UUID.randomUUID(),file.getSize(),u.userId(),Instant.now()); attachments.computeIfAbsent(draftId,k->new ArrayList<>()).add(a); return ResponseEntity.status(HttpStatus.CREATED).body(a);} 
 @GetMapping public List<Attachment> list(@PathVariable UUID draftId){return attachments.getOrDefault(draftId,List.of());}
 @DeleteMapping("/{attachmentId}") public ResponseEntity<Void> delete(@PathVariable UUID draftId,@PathVariable UUID attachmentId){attachments.getOrDefault(draftId,new ArrayList<>()).removeIf(a->a.id().equals(attachmentId)); return ResponseEntity.noContent().build();}
 public record Attachment(UUID id,UUID draftId,String fileName,String contentType,String storageKey,long sizeBytes,UUID createdByUserId,Instant createdAt){}
}
