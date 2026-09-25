import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const ALLOWED_REDIRECTS = new Set([
  "com.inandout.fieldphotoprep.internal://auth-callback",
  "com.inandout.fieldphotoprep://auth-callback",
]);

function json(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

function publishableKey(): string | null {
  const raw = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
  if (!raw) return Deno.env.get("SUPABASE_ANON_KEY") ?? null;
  try {
    const keys = JSON.parse(raw);
    return typeof keys?.default === "string" ? keys.default : null;
  } catch {
    return null;
  }
}

function secretKey(): string | null {
  const raw = Deno.env.get("SUPABASE_SECRET_KEYS");
  if (raw) {
    try {
      const keys = JSON.parse(raw);
      if (typeof keys?.default === "string") return keys.default;
    } catch {
      // Fall through to the legacy server-only key.
    }
  }
  return Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? null;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return json(405, { outcome: "METHOD_NOT_ALLOWED" });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const publicKey = publishableKey();
  const serverKey = secretKey();
  const authorization = req.headers.get("Authorization");

  if (!supabaseUrl || !publicKey || !serverKey) {
    return json(503, { outcome: "SERVER_CONFIGURATION_UNAVAILABLE" });
  }
  if (!authorization?.startsWith("Bearer ")) {
    return json(401, { outcome: "AUTH_REQUIRED" });
  }

  let body: {
    organization_id?: string;
    email?: string;
    intended_role?: string;
    redirect_uri?: string;
  };
  try {
    body = await req.json();
  } catch {
    return json(400, { outcome: "INVALID_REQUEST" });
  }

  const organizationId = body.organization_id?.trim() ?? "";
  const email = body.email?.trim().toLowerCase() ?? "";
  const intendedRole = body.intended_role?.trim().toUpperCase() ?? "";
  const redirectBase = body.redirect_uri?.trim() ?? "";

  if (!organizationId || !email || !intendedRole || !redirectBase) {
    return json(400, { outcome: "INVALID_REQUEST" });
  }
  if (!ALLOWED_REDIRECTS.has(redirectBase)) {
    return json(400, { outcome: "INVALID_REDIRECT" });
  }

  const caller = createClient(supabaseUrl, publicKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: userData, error: userError } = await caller.auth.getUser();
  if (userError || !userData.user) {
    return json(401, { outcome: "AUTH_REQUIRED" });
  }

  const { data: prepared, error: prepareError } = await caller.rpc(
    "fpp_admin_prepare_invitation",
    {
      p_organization_id: organizationId,
      p_email: email,
      p_intended_role: intendedRole,
    },
  );

  if (prepareError) {
    const status = prepareError.code === "42501" ? 403 : 409;
    return json(status, { outcome: status === 403 ? "OWNER_REQUIRED" : "PREPARE_FAILED" });
  }

  const outcome = prepared?.outcome;
  if (outcome !== "CREATED" && outcome !== "RESEND_READY") {
    return json(409, prepared ?? { outcome: "PREPARE_FAILED" });
  }

  const invitationId = prepared?.invitation_id;
  const normalizedEmail = prepared?.email;
  if (typeof invitationId !== "string" || typeof normalizedEmail !== "string") {
    return json(500, { outcome: "PREPARE_RESPONSE_INVALID" });
  }

  const redirectTo =
    `${redirectBase}?fpp_invitation_id=${encodeURIComponent(invitationId)}`;

  const admin = createClient(supabaseUrl, serverKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { error: inviteError } = await admin.auth.admin.inviteUserByEmail(
    normalizedEmail,
    {
      redirectTo,
      data: {
        fpp_invitation_id: invitationId,
        fpp_organization_id: organizationId,
      },
    },
  );

  const { error: recordError } = await caller.rpc(
    "fpp_admin_record_invitation_delivery",
    {
      p_organization_id: organizationId,
      p_invitation_id: invitationId,
      p_succeeded: !inviteError,
    },
  );

  if (recordError) {
    return json(500, {
      outcome: inviteError ? "DELIVERY_FAILED_STATE_UNRECORDED" : "DELIVERY_STATE_UNCERTAIN",
      invitation_id: invitationId,
    });
  }

  if (inviteError) {
    return json(502, {
      outcome: "DELIVERY_FAILED",
      invitation_id: invitationId,
      retryable: true,
    });
  }

  return json(200, {
    outcome: outcome === "RESEND_READY" ? "RESENT" : "SENT",
    invitation_id: invitationId,
    email: normalizedEmail,
    intended_role: prepared?.intended_role,
    expires_at: prepared?.expires_at,
  });
});
