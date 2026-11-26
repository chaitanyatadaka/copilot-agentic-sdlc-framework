import React from 'react';
import {act, cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';
import {api} from './api';
import App from './main';

vi.mock('./api', () => ({api: vi.fn()}));

const queues = [
  {queueId: 12, name: 'Returns', prefix: 'R', open: true},
  {queueId: 13, name: 'Appointments', prefix: 'A', open: false},
];
const queueConfig = {
  queueId: 12,
  name: 'Returns',
  prefix: 'R',
  averageServiceDurationMinutes: 5,
  open: true,
};
const emptyDashboard = {
  queueId: 12,
  counts: {WAITING: 0, CALLED: 0, COMPLETED: 0, SKIPPED: 0, CANCELLED: 0},
  activeTokens: [],
};

beforeEach(() => {
  api.mockReset();
  window.location.hash = '#/customer';
});
afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe('customer experience (T-P4-01–03; REQ-001–006, REQ-021, REQ-037–040, REQ-055)', () => {
  test('lists open queues, joins and shows the server token snapshot', async () => {
    api.mockResolvedValueOnce(queues).mockResolvedValueOnce({
      tokenId: 88,
      displayIdentifier: 'R-1',
      status: 'WAITING',
      position: 1,
      estimatedWaitMinutes: 0,
    });
    render(<App/>);

    expect(await screen.findByRole('heading', {name: 'Join a queue'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Returns'})).toBeInTheDocument();
    expect(screen.queryByRole('option', {name: 'Appointments'})).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Service queue'), {target: {value: '12'}});
    fireEvent.change(screen.getByLabelText('Your name'), {target: {value: 'Sam'}});
    fireEvent.change(screen.getByLabelText(/Phone/), {target: {value: '555-0100'}});
    fireEvent.click(screen.getByRole('button', {name: /Join queue/}));

    expect(await screen.findByRole('heading', {name: 'R-1'})).toBeInTheDocument();
    expect(screen.getByText('YOUR POSITION')).toBeInTheDocument();
    expect(screen.getByText('YOUR POSITION').nextElementSibling).toHaveTextContent('1');
    expect(screen.getByText('EST. WAIT').nextElementSibling).toHaveTextContent('0 min');
    expect(api).toHaveBeenCalledWith('/api/queues/12/join', {
      method: 'POST',
      body: JSON.stringify({customerName: 'Sam', phone: '555-0100'}),
    });
  });

  test('shows join errors without displaying a false token success', async () => {
    api.mockResolvedValueOnce(queues).mockRejectedValueOnce({message: 'Queue is closed.'});
    render(<App/>);
    await screen.findByRole('option', {name: 'Returns'});
    fireEvent.change(screen.getByLabelText('Service queue'), {target: {value: '12'}});
    fireEvent.change(screen.getByLabelText('Your name'), {target: {value: 'Sam'}});
    fireEvent.click(screen.getByRole('button', {name: /Join queue/}));

    expect(await screen.findByRole('alert')).toHaveTextContent('Queue is closed.');
    expect(screen.queryByRole('heading', {name: 'R-1'})).not.toBeInTheDocument();
  });

  test('cancels the waiting token and stops offering cancellation', async () => {
    api.mockResolvedValueOnce(queues)
      .mockResolvedValueOnce({tokenId: 88, displayIdentifier: 'R-1', status: 'WAITING', position: 1, estimatedWaitMinutes: 0})
      .mockResolvedValueOnce({tokenId: 88, displayIdentifier: 'R-1', status: 'CANCELLED', position: null, estimatedWaitMinutes: null});
    render(<App/>);
    await screen.findByRole('option', {name: 'Returns'});
    fireEvent.change(screen.getByLabelText('Service queue'), {target: {value: '12'}});
    fireEvent.change(screen.getByLabelText('Your name'), {target: {value: 'Sam'}});
    fireEvent.click(screen.getByRole('button', {name: /Join queue/}));
    await screen.findByRole('heading', {name: 'R-1'});
    fireEvent.click(screen.getByRole('button', {name: 'Leave this queue'}));

    expect(await screen.findByText('CANCELLED')).toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Leave this queue'})).not.toBeInTheDocument();
    expect(api).toHaveBeenCalledWith('/api/tokens/88/cancel', {method: 'POST'});
  });

  test('polls the token and renders its updated called status', async () => {
    vi.useFakeTimers();
    api.mockResolvedValueOnce(queues)
      .mockResolvedValueOnce({tokenId: 88, displayIdentifier: 'R-1', status: 'WAITING', position: 1, estimatedWaitMinutes: 0})
      .mockResolvedValueOnce({tokenId: 88, displayIdentifier: 'R-1', status: 'CALLED', position: null, estimatedWaitMinutes: null});
    render(<App/>);
    await act(async () => { await Promise.resolve(); });
    fireEvent.change(screen.getByLabelText('Service queue'), {target: {value: '12'}});
    fireEvent.change(screen.getByLabelText('Your name'), {target: {value: 'Sam'}});
    fireEvent.click(screen.getByRole('button', {name: /Join queue/}));
    await act(async () => { await Promise.resolve(); await Promise.resolve(); });
    expect(screen.getByText('WAITING')).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    expect(api).toHaveBeenCalledWith('/api/tokens/88');
    expect(screen.getByText('CALLED')).toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'Leave this queue'})).not.toBeInTheDocument();
  });
});

describe('administrator experience (T-P5-01–03; REQ-007–012, REQ-021, REQ-050–055, REQ-057)', () => {
  beforeEach(() => {
    window.location.hash = '#/admin';
  });

  test('loads queue settings and saves an update through the queue API', async () => {
    api.mockResolvedValueOnce(queues)
      .mockResolvedValueOnce(queueConfig)
      .mockResolvedValueOnce(emptyDashboard)
      .mockResolvedValueOnce({...queueConfig, name: 'Returns Desk', prefix: 'RD', averageServiceDurationMinutes: 8})
      .mockResolvedValueOnce([{...queues[0], name: 'Returns Desk', prefix: 'RD'}])
      .mockResolvedValueOnce(emptyDashboard);
    render(<App/>);

    expect(await screen.findByText('Local demo access — no production authentication')).toBeInTheDocument();
    fireEvent.click(await screen.findByRole('button', {name: /Returns R · OPEN/}));
    expect(await screen.findByRole('heading', {name: 'Configure Returns'})).toBeInTheDocument();
    fireEvent.change(screen.getByRole('textbox', {name: 'Queue name'}), {target: {value: 'Returns Desk'}});
    fireEvent.change(screen.getByRole('textbox', {name: 'Queue prefix'}), {target: {value: 'RD'}});
    fireEvent.change(screen.getByRole('spinbutton', {name: 'Average service duration'}), {target: {value: '8'}});
    fireEvent.click(screen.getByRole('button', {name: 'Save queue changes'}));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/queues/12', {
      method: 'PUT',
      body: JSON.stringify({...queueConfig, name: 'Returns Desk', prefix: 'RD', averageServiceDurationMinutes: 8}),
    }));
    expect(await screen.findByRole('heading', {name: 'Configure Returns Desk'})).toBeInTheDocument();
    expect(screen.getByRole('textbox', {name: 'Queue prefix'})).toHaveValue('RD');
  });

  test('creates a queue and opens/closes it using persisted queue operations', async () => {
    api.mockImplementation(async (path, options = {}) => {
      if (path === '/api/queues' && options.method === 'POST') {
        return {...queueConfig, queueId: 14, name: 'New desk', prefix: 'ND'};
      }
      if (path === '/api/queues') return [...queues, {queueId: 14, name: 'New desk', prefix: 'ND', open: true}];
      if (path === '/api/queues/12/status') return {...queueConfig, open: options.body.includes('"open":false')};
      if (path === '/api/queues/13/status') return {...queueConfig, queueId: 13, open: true};
      throw new Error(`Unexpected API request: ${path}`);
    });
    render(<App/>);

    await screen.findByRole('heading', {name: 'Administrator dashboard'});
    fireEvent.change(screen.getByLabelText('New queue name'), {target: {value: 'New desk'}});
    fireEvent.change(screen.getByLabelText('Ticket prefix'), {target: {value: 'ND'}});
    fireEvent.change(screen.getByLabelText('Average service time (minutes)'), {target: {value: '6'}});
    fireEvent.click(screen.getByRole('button', {name: /Create queue/}));

    expect(await screen.findByText('New desk')).toBeInTheDocument();
    expect(await screen.findByRole('status')).toHaveTextContent('New desk was created successfully.');
    expect(api).toHaveBeenCalledWith('/api/queues', {
      method: 'POST',
      body: JSON.stringify({name: 'New desk', prefix: 'ND', averageServiceDurationMinutes: 6, open: true}),
    });

    fireEvent.click(screen.getByRole('button', {name: 'Close Returns'}));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/queues/12/status', {
      method: 'PATCH',
      body: JSON.stringify({open: false}),
    }));
    fireEvent.click(screen.getByRole('button', {name: 'Open Appointments'}));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/queues/13/status', {
      method: 'PATCH',
      body: JSON.stringify({open: true}),
    }));
  });

  test('shows backend validation details when queue creation is rejected', async () => {
    api.mockResolvedValueOnce(queues).mockRejectedValueOnce({
      status: 400,
      message: 'One or more fields are invalid.',
      fieldErrors: [{field: 'prefix', message: 'must not be blank'}],
    });
    render(<App/>);
    await screen.findByRole('heading', {name: 'Administrator dashboard'});
    fireEvent.change(screen.getByLabelText('New queue name'), {target: {value: 'New desk'}});
    fireEvent.change(screen.getByLabelText('Ticket prefix'), {target: {value: 'ND'}});
    fireEvent.click(screen.getByRole('button', {name: /Create queue/}));

    expect(await screen.findByRole('alert')).toHaveTextContent('One or more fields are invalid.');
    expect(screen.getByRole('alert')).toHaveTextContent('HTTP 400');
    expect(screen.getByRole('alert')).toHaveTextContent('prefix: must not be blank');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(api).toHaveBeenCalledWith('/api/queues', {
      method: 'POST',
      body: JSON.stringify({name: 'New desk', prefix: 'ND', averageServiceDurationMinutes: 5, open: true}),
    });
  });

  test('shows all dashboard totals and only exposes complete/skip for a called token', async () => {
    api.mockResolvedValueOnce(queues)
      .mockResolvedValueOnce(queueConfig)
      .mockResolvedValueOnce({
        ...emptyDashboard,
        counts: {WAITING: 2, CALLED: 1, COMPLETED: 4, SKIPPED: 1, CANCELLED: 3},
        activeTokens: [
          {tokenId: 1, displayIdentifier: 'R-1', status: 'WAITING'},
          {tokenId: 2, displayIdentifier: 'R-2', status: 'CALLED'},
        ],
      });
    render(<App/>);
    fireEvent.click(await screen.findByRole('button', {name: /Returns R · OPEN/}));

    expect(await screen.findByRole('heading', {name: 'Dashboard'})).toBeInTheDocument();
    for (const [label, count] of Object.entries({Waiting: 2, Called: 1, Completed: 4, Skipped: 1, Cancelled: 3})) {
      expect(screen.getByText(label).previousSibling).toHaveTextContent(String(count));
    }
    expect(screen.getByText('R-2')).toBeInTheDocument();
    expect(screen.getByText('CALLED')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Complete'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Skip'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Call next'})).toBeInTheDocument();
  });

  test('calls next and sends complete/skip only for the called token', async () => {
    const calledDashboard = {
      ...emptyDashboard,
      counts: {WAITING: 1, CALLED: 1, COMPLETED: 0, SKIPPED: 0, CANCELLED: 0},
      activeTokens: [
        {tokenId: 21, displayIdentifier: 'R-1', status: 'WAITING'},
        {tokenId: 22, displayIdentifier: 'R-2', status: 'CALLED'},
      ],
    };
    api.mockImplementation(async (path, options = {}) => {
      if (path === '/api/queues') return queues;
      if (path === '/api/queues/12') return queueConfig;
      if (path === '/api/queues/12/dashboard') return calledDashboard;
      if (path === '/api/queues/12/call-next' || path === '/api/tokens/22/complete' || path === '/api/tokens/22/skip') {
        expect(options.method).toBe('POST');
        return {tokenId: 22, status: 'CALLED'};
      }
      throw new Error(`Unexpected API request: ${path}`);
    });
    render(<App/>);
    fireEvent.click(await screen.findByRole('button', {name: /Returns R · OPEN/}));
    await screen.findByText('R-2');
    expect(screen.getByText('CALLED')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: 'Call next'}));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/queues/12/call-next', {method: 'POST'}));
    fireEvent.click(screen.getByRole('button', {name: 'Complete'}));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/tokens/22/complete', {method: 'POST'}));
    fireEvent.click(screen.getByRole('button', {name: 'Skip'}));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/tokens/22/skip', {method: 'POST'}));
    expect(api.mock.calls.some(([path]) => path === '/api/tokens/21/complete' || path === '/api/tokens/21/skip')).toBe(false);
  });

  test('renders administrator update conflicts without showing updated state', async () => {
    api.mockResolvedValueOnce(queues)
      .mockResolvedValueOnce(queueConfig)
      .mockResolvedValueOnce(emptyDashboard)
      .mockRejectedValueOnce({message: 'Queue prefix already exists.'});
    render(<App/>);
    fireEvent.click(await screen.findByRole('button', {name: /Returns R · OPEN/}));
    await screen.findByRole('heading', {name: 'Configure Returns'});
    fireEvent.change(screen.getByRole('textbox', {name: 'Queue prefix'}), {target: {value: 'A'}});
    fireEvent.click(screen.getByRole('button', {name: 'Save queue changes'}));

    expect(await screen.findByRole('alert')).toHaveTextContent('Queue prefix already exists.');
    expect(screen.getByRole('textbox', {name: 'Queue prefix'})).toHaveValue('A');
    expect(api).toHaveBeenCalledWith('/api/queues/12', expect.objectContaining({method: 'PUT'}));
  });
});
