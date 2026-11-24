// A recording belongs to one editable draft. Only the form can submit it.
export function dictation(editor, mic, discard, status) {
  const formats = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4', 'audio/webm', 'audio/ogg'];
  const mime = window.MediaRecorder?.isTypeSupported && formats.find(type => MediaRecorder.isTypeSupported(type));
  const supported = window.isSecureContext && navigator.mediaDevices?.getUserMedia && mime;
  const unavailable = 'سرویس گفتار در دسترس نیست. می‌توانید تایپ کنید.';
  let ready = false, checking = false, current, notice = '', retryAt = 0;

  function render() {
    const phase = current?.phase;
    mic.disabled = phase !== 'recording' && (!!phase || !ready || Date.now() < retryAt);
    const label = phase === 'recording' ? 'توقف ضبط' : 'میکروفون';
    if (mic.textContent !== label) mic.textContent = label;
    discard.hidden = !phase;
    const text = notice || (!supported ? (window.isSecureContext
      ? 'این مرورگر از ضبط صدا پشتیبانی نمی‌کند.' : 'برای ضبط صدا از HTTPS یا پورت فورواردشده روی localhost استفاده کنید.')
      : !ready ? unavailable : 'برای گفتن پیام، میکروفون را بزنید.');
    if (status.textContent !== text) status.textContent = text;
  }
  async function health() {
    if (!supported || checking || document.hidden) return;
    checking = true;
    const previous = ready;
    try {
      const response = await fetch('/speech/health', {cache:'no-store', credentials:'omit', signal:AbortSignal.timeout(3500)});
      const data = response.ok && await response.json();
      ready = data?.status === 'ok' && data.model_loaded === true;
    } catch { ready = false; }
    finally {
      checking = false;
      if (!current && previous !== ready && Date.now() >= retryAt) notice = '';
      render();
    }
  }
  function finish(message = '') {
    const op = current;
    current = null;
    if (op) {
      clearTimeout(op.timer);
      op.controller.abort();
      if (op.recorder?.state === 'recording') op.recorder.stop();
      op.stream?.getTracks().forEach(track => track.stop());
      op.chunks.length = 0;
    }
    notice = message;
    render();
  }
  const cancel = () => { if (current) finish(); };

  async function upload(op) {
    if (op !== current) return;
    clearTimeout(op.timer);
    op.stream.getTracks().forEach(track => track.stop());
    const blob = new Blob(op.chunks, {type:mime.split(';')[0]});
    op.chunks.length = 0;
    if (!blob.size) return finish('صدایی ضبط نشد. دوباره ضبط کنید.');
    op.phase = 'upload';
    notice = 'در حال تبدیل صدا به متن…';
    render();
    const form = new FormData();
    form.append('file', blob, 'dictation.' + blob.type.split('/')[1]);
    op.timer = setTimeout(() => finish('زمان انتظار تمام شد. دوباره ضبط کنید.'), 365000);
    try {
      const response = await fetch('/speech/transcribe', {method:'POST', body:form, credentials:'omit',
        redirect:'error', signal:op.controller.signal});
      if (op !== current) return;
      if (response.status === 429) {
        const retry = response.headers.get('Retry-After') || '1';
        retryAt = /^\d+$/.test(retry) ? Date.now() + Number(retry) * 1000 : Date.parse(retry) || Date.now();
        const delay = Math.max(0, retryAt - Date.now());
        setTimeout(() => { if (!current && Date.now() >= retryAt) { notice = ''; render(); } }, Math.min(delay + 1, 2147483647));
        return finish('سرویس مشغول است. پس از کمی مکث دوباره ضبط کنید.');
      }
      if (response.status === 504) return finish('زمان انتظار تمام شد. دوباره ضبط کنید.');
      if (!response.ok) throw Error();
      const data = await response.json();
      if (op !== current) return;
      if (typeof data.text !== 'string' || !data.text.trim()) return finish('گفتاری تشخیص داده نشد. دوباره ضبط کنید.');
      const end = editor.value.length;
      editor.setRangeText((end && !/\s$/.test(editor.value) ? ' ' : '') + data.text.trim(), end, end, 'preserve');
      finish('متن اضافه شد. آن را ویرایش و ارسال کنید.');
    } catch { if (op === current) finish('تبدیل صدا انجام نشد. دوباره ضبط کنید.'); }
  }
  function stop(op) {
    if (current !== op || op.phase !== 'recording') return;
    op.phase = 'stopping';
    clearTimeout(op.timer);
    render();
    op.timer = setTimeout(() => finish('ضبط کامل نشد. دوباره ضبط کنید.'), 3000);
    op.recorder.stop();
  }
  mic.onclick = async () => {
    if (current?.phase === 'recording') {
      editor.focus({preventScroll:true});
      return stop(current);
    }
    if (mic.disabled || current) return;
    const op = {phase:'permission', controller:new AbortController(), chunks:[]};
    current = op;
    notice = 'در انتظار اجازهٔ میکروفون…';
    render();
    try {
      const stream = await navigator.mediaDevices.getUserMedia({audio:true});
      if (current !== op) { stream.getTracks().forEach(track => track.stop()); return; }
      op.stream = stream;
      const recorder = new MediaRecorder(stream, {mimeType:mime, audioBitsPerSecond:96000});
      op.recorder = recorder;
      recorder.ondataavailable = event => { if (op === current && event.data.size) op.chunks.push(event.data); };
      recorder.onstop = () => upload(op);
      recorder.onerror = () => { if (op === current) finish('ضبط کامل نشد. دوباره ضبط کنید.'); };
      recorder.start();
      op.phase = 'recording';
      notice = 'در حال ضبط صدا؛ حداکثر ۴۵ ثانیه';
      op.timer = setTimeout(() => stop(op), 45000);
      render();
    } catch (error) {
      if (op === current) finish(error.name === 'NotAllowedError'
        ? 'اجازهٔ ضبط داده نشد. دسترسی میکروفون را در مرورگر بررسی کنید.' : 'میکروفون آماده نیست. اتصال آن را بررسی کنید.');
    }
  };
  discard.onclick = cancel;
  editor.addEventListener('beforeinput', () => {
    if (editor.value && editor.selectionStart === 0 && editor.selectionEnd === editor.value.length) cancel();
  });
  editor.addEventListener('input', () => { if (!editor.value) cancel(); });
  document.addEventListener('visibilitychange', () => { if (document.hidden) cancel(); else health(); });
  window.addEventListener('pagehide', cancel);
  window.addEventListener('online', health);
  window.addEventListener('offline', () => { ready = false; finish(unavailable); });
  setInterval(health, 30000);
  render();
  health();
  return cancel;
}
