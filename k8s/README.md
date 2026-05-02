# TalkAR Kubernetes Deploy Quick Guide

This is the minimal, executable deployment order for the current manifests in `k8s/`.

## Prerequisites
- `kubectl` configured for target cluster
- Namespace created (example below uses `talkar-production`)
- Required secrets created (`talkar-secrets`)
- Container images already pushed and referenced by manifests

## Apply Order
Run in this exact order:

```bash
# 1) Database
kubectl apply -f k8s/database-deployment.yaml -n talkar-production

# 2) Backend (API + HPA + service)
kubectl apply -f k8s/backend-deployment.yaml -n talkar-production

# 2.1) Talking-photo worker + HPA (required for SQS processing)
kubectl apply -f k8s/talking-photo-worker-deployment.yaml -n talkar-production

# 2.2) Optional KEDA autoscaling from SQS queue depth
kubectl apply -f k8s/talking-photo-worker-keda-scaledobject.yaml -n talkar-production

# 3) Frontend (dashboard + service + frontend ingress if used)
kubectl apply -f k8s/frontend-deployment.yaml -n talkar-production

# 4) ALB ingress for backend API (strict worker-auth health checks)
kubectl apply -f k8s/ingress.yaml -n talkar-production
```

## Post-Deploy Verification

```bash
# Overall workload health
kubectl get pods -n talkar-production
kubectl get svc -n talkar-production
kubectl get ingress -n talkar-production
kubectl get hpa -n talkar-production
kubectl get scaledobject -n talkar-production

# Backend readiness (should become 1/1 for each pod)
kubectl get pods -n talkar-production -l app=talkar-backend -w

# Backend strict health probe response (from inside cluster)
kubectl run -it curl-check --rm --restart=Never --image=curlimages/curl -- \
  curl -sS http://talkar-backend-service:3000/health?workerAuthStrict=true
```

## Fast Troubleshooting

```bash
# Backend logs
kubectl logs -n talkar-production -l app=talkar-backend --tail=200

# Worker logs
kubectl logs -n talkar-production -l app=talkar-talking-photo-worker --tail=200

# Describe failing backend pod
kubectl describe pod -n talkar-production <backend-pod-name>

# Check ingress events
kubectl describe ingress -n talkar-production talkar-backend-alb-ingress
```

## Notes
- Backend readiness uses `/health?workerAuthStrict=true`.
- If worker-auth thresholds are breached, readiness can fail intentionally (`503`) until recovered.
- Worker drain mode can be enabled with `TALKING_PHOTO_DRAIN_MODE=true`.
- See:
  - `docs/DEPLOYMENT.md`
  - `docs/WORKER_AUTH_INCIDENT_RUNBOOK.md`
  - `docs/PHASE3_GA_EXECUTION_RUNBOOK.md`
